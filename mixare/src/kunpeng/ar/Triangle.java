package kunpeng.ar;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Triangle {
	private final static int VERTS = 3;

	private FloatBuffer mFVertexBuffer;
	private ShortBuffer mIndexBuffer;
	// A unit-sided equalateral triangle centered on the origin.
	private static float[] sCoords = { 0.0f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f,
		-1.0f, 0.0f };
	public Triangle(float[] coords) {

		// Buffers to be passed to gl*Pointer() functions
		// must be direct, i.e., they must be placed on the
		// native heap where the garbage collector cannot
		// move them.
		//
		// Buffers with multi-byte datatypes (e.g., short, int, float)
		// must have their byte order set to native order

		sCoords = coords;
		
		ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mFVertexBuffer = vbb.asFloatBuffer();

		ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());

		ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
		ibb.order(ByteOrder.nativeOrder());
		mIndexBuffer = ibb.asShortBuffer();

		for (int i = 0; i < VERTS; i++) {
			for (int j = 0; j < 3; j++) {
				mFVertexBuffer.put(sCoords[i * 3 + j]);
			}
		}
		for (int i = 0; i < VERTS; i++) {
			mIndexBuffer.put((short) i);
		}

		mFVertexBuffer.position(0);
		mIndexBuffer.position(0);
	}

	public void draw(GL10 gl) {
		gl.glFrontFace(GL10.GL_CCW);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDrawElements(GL10.GL_LINE_LOOP, VERTS,
				GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
	}

	public float getX(int vertex) {
		return sCoords[3 * vertex];
	}

	public float getY(int vertex) {
		return sCoords[3 * vertex + 1];
	}

}