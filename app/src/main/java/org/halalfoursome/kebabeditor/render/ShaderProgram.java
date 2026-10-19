package org.halalfoursome.kebabeditor.render;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.math.Mat4;

import com.jogamp.opengl.GL3;

public final class ShaderProgram {

    private final int id;
    private final Map<String, Integer> locations = new HashMap<>();
    private final float[] matrix = new float[16];

    private ShaderProgram(int id) {
        this.id = id;
    }

    /** Loads {@code /shaders/<name>.vert} and {@code /shaders/<name>.frag} from the classpath. */
    public static ShaderProgram load(GL3 gl, String name) {
        return compile(gl, name, resource(name + ".vert"), resource(name + ".frag"));
    }

    public static ShaderProgram compile(GL3 gl, String name, String vertexSource, String fragmentSource) {
        int vertex = compileStage(gl, name, GL3.GL_VERTEX_SHADER, vertexSource);
        int fragment = compileStage(gl, name, GL3.GL_FRAGMENT_SHADER, fragmentSource);

        int program = gl.glCreateProgram();
        gl.glAttachShader(program, vertex);
        gl.glAttachShader(program, fragment);
        gl.glLinkProgram(program);

        // Shaders are owned by the program once linked.
        gl.glDetachShader(program, vertex);
        gl.glDetachShader(program, fragment);
        gl.glDeleteShader(vertex);
        gl.glDeleteShader(fragment);

        int[] status = new int[1];
        gl.glGetProgramiv(program, GL3.GL_LINK_STATUS, status, 0);
        if (status[0] == GL3.GL_FALSE) {
            String log = programLog(gl, program);
            gl.glDeleteProgram(program);
            throw new IllegalStateException("Shader '" + name + "' failed to link:\n" + log);
        }

        return new ShaderProgram(program);
    }

    public void use(GL3 gl) {
        gl.glUseProgram(id);
    }

    public void setMat4(GL3 gl, String uniform, Mat4 value) {
        gl.glUniformMatrix4fv(location(gl, uniform), 1, false, value.toArray(matrix), 0);
    }

    public void setVec4(GL3 gl, String uniform, float x, float y, float z, float w) {
        gl.glUniform4f(location(gl, uniform), x, y, z, w);
    }

    public void setVec3(GL3 gl, String uniform, float x, float y, float z) {
        gl.glUniform3f(location(gl, uniform), x, y, z);
    }

    public void setFloat(GL3 gl, String uniform, float value) {
        gl.glUniform1f(location(gl, uniform), value);
    }

    public void setInt(GL3 gl, String uniform, int value) {
        gl.glUniform1i(location(gl, uniform), value);
    }

    public void setBool(GL3 gl, String uniform, boolean value) {
        setInt(gl, uniform, value ? 1 : 0);
    }

    public void dispose(GL3 gl) {
        gl.glDeleteProgram(id);
        locations.clear();
    }

    // -1 for uniforms the compiler optimized away; GL silently ignores writes to -1.
    private int location(GL3 gl, String uniform) {
        return locations.computeIfAbsent(uniform, u -> gl.glGetUniformLocation(id, u));
    }

    private static int compileStage(GL3 gl, String name, int type, String source) {
        int shader = gl.glCreateShader(type);
        gl.glShaderSource(shader, 1, new String[] { source }, null, 0);
        gl.glCompileShader(shader);

        int[] status = new int[1];
        gl.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GL3.GL_FALSE) {
            int[] length = new int[1];
            gl.glGetShaderiv(shader, GL3.GL_INFO_LOG_LENGTH, length, 0);
            byte[] log = new byte[Math.max(length[0], 1)];
            gl.glGetShaderInfoLog(shader, log.length, null, 0, log, 0);
            gl.glDeleteShader(shader);

            String stage = type == GL3.GL_VERTEX_SHADER ? "vertex" : "fragment";
            throw new IllegalStateException(
                "Shader '" + name + "' (" + stage + ") failed to compile:\n"
                    + new String(log, StandardCharsets.UTF_8).trim()
            );
        }

        return shader;
    }

    private static String programLog(GL3 gl, int program) {
        int[] length = new int[1];
        gl.glGetProgramiv(program, GL3.GL_INFO_LOG_LENGTH, length, 0);
        byte[] log = new byte[Math.max(length[0], 1)];
        gl.glGetProgramInfoLog(program, log.length, null, 0, log, 0);
        return new String(log, StandardCharsets.UTF_8).trim();
    }

    private static String resource(String file) {
        String path = "/shaders/" + file;

        try (InputStream stream = ShaderProgram.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing shader resource " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read shader resource " + path, e);
        }
    }
}
