/*
 * Ported from imgui/gl3/ImGuiImplGl3.java of imgui-java 1.92.7.1 (https://github.com/SpaiR/imgui-java), itself a
 * port of backends/imgui_impl_opengl3.cpp of Dear ImGui v1.92.7 (https://github.com/ocornut/imgui).
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2026 Omar Cornut
 * Copyright (c) 2019-present, Ilya "SpaiR" Prymshyts
 * Copyright (c) 2026 Mikołaj Nowak
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ivan1pl.lorisui.imgui.gles3;

import imgui.ImDrawData;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec4;
import imgui.type.ImInt;
import org.lwjgl.opengles.GLES30;

import java.nio.ByteBuffer;

/**
 * Dear ImGui renderer backend for OpenGL ES 3.0. It draws without a base vertex, which OpenGL ES 3.0 lacks; the
 * resulting per-window vertex limit is documented on {@code ImGuiSdlGles3Renderer}.
 */
public final class ImGuiImplGles3 {
    private static final String RENDERER_NAME = "lorisui_impl_opengles3";
    private static final String VERTEX_SHADER = """
            #version 300 es
            precision highp float;
            layout (location = 0) in vec2 Position;
            layout (location = 1) in vec2 UV;
            layout (location = 2) in vec4 Color;
            uniform mat4 ProjMtx;
            out vec2 Frag_UV;
            out vec4 Frag_Color;
            void main()
            {
                Frag_UV = UV;
                Frag_Color = Color;
                gl_Position = ProjMtx * vec4(Position.xy,0,1);
            }
            """;
    private static final String FRAGMENT_SHADER = """
            #version 300 es
            precision mediump float;
            uniform sampler2D Texture;
            in vec2 Frag_UV;
            in vec4 Frag_Color;
            layout (location = 0) out vec4 Out_Color;
            void main()
            {
                Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
            }
            """;
    private static final int VERTEX_UV_OFFSET = 8;
    private static final int VERTEX_COLOR_OFFSET = 16;

    private final ImVec4 clipRect = new ImVec4();
    private final float[] projection = new float[16];
    private final int[] lastViewport = new int[4];
    private final int[] lastScissorBox = new int[4];
    private int vertexShader;
    private int fragmentShader;
    private int program;
    private int uniformLocationTexture;
    private int uniformLocationProjection;
    private int attribLocationPosition;
    private int attribLocationUv;
    private int attribLocationColor;
    private int vertexBuffer;
    private int elementBuffer;
    private int fontTexture;

    public void init() {
        ImGuiIO io = ImGui.getIO();
        io.setBackendRendererName(RENDERER_NAME);

        int lastTexture = GLES30.glGetInteger(GLES30.GL_TEXTURE_BINDING_2D);
        int lastPixelUnpackBuffer = GLES30.glGetInteger(GLES30.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int lastUnpackAlignment = GLES30.glGetInteger(GLES30.GL_UNPACK_ALIGNMENT);
        int lastUnpackRowLength = GLES30.glGetInteger(GLES30.GL_UNPACK_ROW_LENGTH);
        int lastUnpackSkipPixels = GLES30.glGetInteger(GLES30.GL_UNPACK_SKIP_PIXELS);
        int lastUnpackSkipRows = GLES30.glGetInteger(GLES30.GL_UNPACK_SKIP_ROWS);
        try {
            GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, 0);
            createProgram();
            vertexBuffer = GLES30.glGenBuffers();
            elementBuffer = GLES30.glGenBuffers();
            createFontTexture(io.getFonts());
        } finally {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lastTexture);
            GLES30.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, lastPixelUnpackBuffer);
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, lastUnpackAlignment);
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, lastUnpackRowLength);
            GLES30.glPixelStorei(GLES30.GL_UNPACK_SKIP_PIXELS, lastUnpackSkipPixels);
            GLES30.glPixelStorei(GLES30.GL_UNPACK_SKIP_ROWS, lastUnpackSkipRows);
        }
    }

    public void renderDrawData(ImDrawData drawData) {
        int fbWidth = (int) (drawData.getDisplaySizeX() * drawData.getFramebufferScaleX());
        int fbHeight = (int) (drawData.getDisplaySizeY() * drawData.getFramebufferScaleY());
        if (fbWidth <= 0 || fbHeight <= 0 || drawData.getCmdListsCount() <= 0) {
            return;
        }

        int lastActiveTexture = GLES30.glGetInteger(GLES30.GL_ACTIVE_TEXTURE);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        int lastProgram = GLES30.glGetInteger(GLES30.GL_CURRENT_PROGRAM);
        int lastTexture = GLES30.glGetInteger(GLES30.GL_TEXTURE_BINDING_2D);
        int lastSampler = GLES30.glGetInteger(GLES30.GL_SAMPLER_BINDING);
        int lastArrayBuffer = GLES30.glGetInteger(GLES30.GL_ARRAY_BUFFER_BINDING);
        int lastVertexArray = GLES30.glGetInteger(GLES30.GL_VERTEX_ARRAY_BINDING);
        GLES30.glGetIntegerv(GLES30.GL_VIEWPORT, lastViewport);
        GLES30.glGetIntegerv(GLES30.GL_SCISSOR_BOX, lastScissorBox);
        int lastBlendSrcRgb = GLES30.glGetInteger(GLES30.GL_BLEND_SRC_RGB);
        int lastBlendDstRgb = GLES30.glGetInteger(GLES30.GL_BLEND_DST_RGB);
        int lastBlendSrcAlpha = GLES30.glGetInteger(GLES30.GL_BLEND_SRC_ALPHA);
        int lastBlendDstAlpha = GLES30.glGetInteger(GLES30.GL_BLEND_DST_ALPHA);
        int lastBlendEquationRgb = GLES30.glGetInteger(GLES30.GL_BLEND_EQUATION_RGB);
        int lastBlendEquationAlpha = GLES30.glGetInteger(GLES30.GL_BLEND_EQUATION_ALPHA);
        boolean lastEnableBlend = GLES30.glIsEnabled(GLES30.GL_BLEND);
        boolean lastEnableCullFace = GLES30.glIsEnabled(GLES30.GL_CULL_FACE);
        boolean lastEnableDepthTest = GLES30.glIsEnabled(GLES30.GL_DEPTH_TEST);
        boolean lastEnableStencilTest = GLES30.glIsEnabled(GLES30.GL_STENCIL_TEST);
        boolean lastEnableScissorTest = GLES30.glIsEnabled(GLES30.GL_SCISSOR_TEST);

        // A VAO per draw, because VAOs are not shared between GL contexts
        int vertexArray = GLES30.glGenVertexArrays();
        try {
            setupRenderState(drawData, fbWidth, fbHeight, vertexArray);
            drawCommandLists(drawData, fbHeight);
        } finally {
            GLES30.glDeleteVertexArrays(vertexArray);

            // A program that was pending deletion when it was saved is gone by now, and binding it would be an error
            if (lastProgram == 0 || GLES30.glIsProgram(lastProgram)) {
                GLES30.glUseProgram(lastProgram);
            }
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lastTexture);
            GLES30.glBindSampler(0, lastSampler);
            GLES30.glActiveTexture(lastActiveTexture);
            GLES30.glBindVertexArray(lastVertexArray);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lastArrayBuffer);
            GLES30.glBlendEquationSeparate(lastBlendEquationRgb, lastBlendEquationAlpha);
            GLES30.glBlendFuncSeparate(lastBlendSrcRgb, lastBlendDstRgb, lastBlendSrcAlpha, lastBlendDstAlpha);
            setEnabled(GLES30.GL_BLEND, lastEnableBlend);
            setEnabled(GLES30.GL_CULL_FACE, lastEnableCullFace);
            setEnabled(GLES30.GL_DEPTH_TEST, lastEnableDepthTest);
            setEnabled(GLES30.GL_STENCIL_TEST, lastEnableStencilTest);
            setEnabled(GLES30.GL_SCISSOR_TEST, lastEnableScissorTest);
            GLES30.glViewport(lastViewport[0], lastViewport[1], lastViewport[2], lastViewport[3]);
            GLES30.glScissor(lastScissorBox[0], lastScissorBox[1], lastScissorBox[2], lastScissorBox[3]);
        }
    }

    public void shutdown() {
        if (vertexBuffer != 0) {
            GLES30.glDeleteBuffers(vertexBuffer);
            vertexBuffer = 0;
        }
        if (elementBuffer != 0) {
            GLES30.glDeleteBuffers(elementBuffer);
            elementBuffer = 0;
        }
        if (program != 0) {
            GLES30.glDeleteProgram(program);
            program = 0;
        }
        deleteShaders();
        if (fontTexture != 0) {
            GLES30.glDeleteTextures(fontTexture);
            fontTexture = 0;
        }
        ImGuiIO io = ImGui.getIO();
        io.getFonts().setTexID(0);
        io.setBackendRendererName(null);
    }

    private void createProgram() {
        vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER, "vertex");
        fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER, "fragment");

        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);
        if (GLES30.glGetProgrami(program, GLES30.GL_LINK_STATUS) == GLES30.GL_FALSE) {
            throw new IllegalStateException("Dear ImGui OpenGL ES shader program failed to link: "
                    + GLES30.glGetProgramInfoLog(program));
        }
        GLES30.glDetachShader(program, vertexShader);
        GLES30.glDetachShader(program, fragmentShader);
        deleteShaders();

        uniformLocationTexture = GLES30.glGetUniformLocation(program, "Texture");
        uniformLocationProjection = GLES30.glGetUniformLocation(program, "ProjMtx");
        attribLocationPosition = GLES30.glGetAttribLocation(program, "Position");
        attribLocationUv = GLES30.glGetAttribLocation(program, "UV");
        attribLocationColor = GLES30.glGetAttribLocation(program, "Color");
    }

    private static int compileShader(int type, String source, String name) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        if (GLES30.glGetShaderi(shader, GLES30.GL_COMPILE_STATUS) == GLES30.GL_FALSE) {
            String log = GLES30.glGetShaderInfoLog(shader);
            GLES30.glDeleteShader(shader);
            throw new IllegalStateException("Dear ImGui OpenGL ES " + name + " shader failed to compile: " + log);
        }
        return shader;
    }

    private void deleteShaders() {
        if (vertexShader != 0) {
            GLES30.glDeleteShader(vertexShader);
            vertexShader = 0;
        }
        if (fragmentShader != 0) {
            GLES30.glDeleteShader(fragmentShader);
            fragmentShader = 0;
        }
    }

    private void createFontTexture(ImFontAtlas fontAtlas) {
        ImInt width = new ImInt();
        ImInt height = new ImInt();
        ByteBuffer pixels = fontAtlas.getTexDataAsRGBA32(width, height);

        fontTexture = GLES30.glGenTextures();
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fontTexture);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1);
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0);
        GLES30.glPixelStorei(GLES30.GL_UNPACK_SKIP_PIXELS, 0);
        GLES30.glPixelStorei(GLES30.GL_UNPACK_SKIP_ROWS, 0);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width.get(), height.get(), 0, GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE, pixels);

        fontAtlas.setTexID(fontTexture);
    }

    private void setupRenderState(ImDrawData drawData, int fbWidth, int fbHeight, int vertexArray) {
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendEquation(GLES30.GL_FUNC_ADD);
        GLES30.glBlendFuncSeparate(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA, GLES30.GL_ONE,
                GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_STENCIL_TEST);
        GLES30.glEnable(GLES30.GL_SCISSOR_TEST);

        GLES30.glViewport(0, 0, fbWidth, fbHeight);
        float left = drawData.getDisplayPosX();
        float right = drawData.getDisplayPosX() + drawData.getDisplaySizeX();
        float top = drawData.getDisplayPosY();
        float bottom = drawData.getDisplayPosY() + drawData.getDisplaySizeY();
        projection[0] = 2.0f / (right - left);
        projection[5] = 2.0f / (top - bottom);
        projection[10] = -1.0f;
        projection[12] = (right + left) / (left - right);
        projection[13] = (top + bottom) / (bottom - top);
        projection[15] = 1.0f;

        GLES30.glUseProgram(program);
        GLES30.glUniform1i(uniformLocationTexture, 0);
        GLES30.glUniformMatrix4fv(uniformLocationProjection, false, projection);
        // Textures carry their own sampling state; an application sampler on unit 0 would override it
        GLES30.glBindSampler(0, 0);

        GLES30.glBindVertexArray(vertexArray);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBuffer);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
        GLES30.glEnableVertexAttribArray(attribLocationPosition);
        GLES30.glEnableVertexAttribArray(attribLocationUv);
        GLES30.glEnableVertexAttribArray(attribLocationColor);
        int stride = ImDrawData.sizeOfImDrawVert();
        GLES30.glVertexAttribPointer(attribLocationPosition, 2, GLES30.GL_FLOAT, false, stride, 0L);
        GLES30.glVertexAttribPointer(attribLocationUv, 2, GLES30.GL_FLOAT, false, stride, VERTEX_UV_OFFSET);
        GLES30.glVertexAttribPointer(attribLocationColor, 4, GLES30.GL_UNSIGNED_BYTE, true, stride,
                VERTEX_COLOR_OFFSET);
    }

    private void drawCommandLists(ImDrawData drawData, int fbHeight) {
        float clipOffX = drawData.getDisplayPosX();
        float clipOffY = drawData.getDisplayPosY();
        float clipScaleX = drawData.getFramebufferScaleX();
        float clipScaleY = drawData.getFramebufferScaleY();
        int indexSize = ImDrawData.sizeOfImDrawIdx();
        int indexType = indexSize == 2 ? GLES30.GL_UNSIGNED_SHORT : GLES30.GL_UNSIGNED_INT;

        for (int list = 0; list < drawData.getCmdListsCount(); list++) {
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, drawData.getCmdListVtxBufferData(list),
                    GLES30.GL_STREAM_DRAW);
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, drawData.getCmdListIdxBufferData(list),
                    GLES30.GL_STREAM_DRAW);

            for (int command = 0; command < drawData.getCmdListCmdBufferSize(list); command++) {
                drawData.getCmdListCmdBufferClipRect(clipRect, list, command);
                float clipMinX = (clipRect.x - clipOffX) * clipScaleX;
                float clipMinY = (clipRect.y - clipOffY) * clipScaleY;
                float clipMaxX = (clipRect.z - clipOffX) * clipScaleX;
                float clipMaxY = (clipRect.w - clipOffY) * clipScaleY;
                if (clipMaxX <= clipMinX || clipMaxY <= clipMinY) {
                    continue;
                }

                // Y is inverted in OpenGL ES
                GLES30.glScissor((int) clipMinX, (int) (fbHeight - clipMaxY), (int) (clipMaxX - clipMinX),
                        (int) (clipMaxY - clipMinY));

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,
                        (int) drawData.getCmdListCmdBufferTextureId(list, command));
                long indexOffset = drawData.getCmdListCmdBufferIdxOffset(list, command) * (long) indexSize;
                GLES30.glDrawElements(GLES30.GL_TRIANGLES, drawData.getCmdListCmdBufferElemCount(list, command),
                        indexType, indexOffset);
            }
        }
    }

    private static void setEnabled(int capability, boolean enabled) {
        if (enabled) {
            GLES30.glEnable(capability);
        } else {
            GLES30.glDisable(capability);
        }
    }
}
