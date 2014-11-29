/*
 *  Copyright (c) 2013-2014 twinlife SAS.
 *
 *  All Rights Reserved.
 *  
 *  Contributor: Christian Jacquemot (Christian.Jacquemot@twinlife-systems.com)
 *  Contributor: Xiaobo Xie (Xiaobo.Xie@twinlife-systems.com)
 *  
 *  based on org.appspot.apprtc.VideoStreamsView
 */

/*
 * libjingle
 * Copyright 2013, Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.blueprint.outbound;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.I420Frame;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;

/**
 * A GLSurfaceView{,.Renderer} that efficiently renders YUV frames from local &
 * remote VideoTracks using the GPU.
 */
public class VideoStreamsView extends GLSurfaceView implements
		GLSurfaceView.Renderer {
	private final static String LOG_TAG = "VideoStreamsView";
	private final static boolean DEBUG = false;

	private final static long MAX_FPS = 1000 / 30; // 30 FPS

	private enum Orientation {
		LANDSCAPE, PORTRAIT
	};

	private enum VideoRendererIndex {
		MAIN_VIDEO, THUMBNAIL_VIDEO_0, THUMBNAIL_VIDEO_1, THUMBNAIL_VIDEO_2
	}

	private class VideoStreamRenderer implements VideoRenderer.Callbacks {
		private final static int MAX_ALLOCATED_FRAMES = 2;

		private VideoRendererIndex mIndex;
		private VideoRenderer mVideoRenderer;
		private int mFrameWidth;
		private int mFrameHeight;
		LinkedList<I420Frame> mFramePool = new LinkedList<I420Frame>();
		private int mAllocatedFrames = 0;

		public VideoStreamRenderer(VideoRendererIndex index) {
			mIndex = index;

			mVideoRenderer = new VideoRenderer(this);
		}

		public VideoRenderer getVideoRenderer() {

			return mVideoRenderer;
		}

		@Override
		public void setSize(final int width, final int height) {
			queueEvent(new Runnable() {
				public void run() {

					if (DEBUG) {
						Log.d(LOG_TAG, "VideoStreamRenderer::setSize: width="
								+ width + " height=" + height);
					}

					mFrameWidth = width;
					mFrameHeight = height;

					setSize();

					int index = mIndex.ordinal();

					// -CJ- TBD release texture

					// Generate 3 texture ids for Y/U/V and place them into
					// |textures|,
					// allocating enough storage for |width|x|height| pixels.
					int[] textures = mTextures[index];
					GLES20.glGenTextures(3, textures, 0);
					for (int i = 0; i < 3; ++i) {
						int w = i == 0 ? mFrameWidth : mFrameWidth / 2;
						int h = i == 0 ? mFrameHeight : mFrameHeight / 2;
						GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
						GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
						GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
								GLES20.GL_LUMINANCE, w, h, 0,
								GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
								null);
						GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
								GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
						GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
								GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
						GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
								GLES20.GL_TEXTURE_WRAP_S,
								GLES20.GL_CLAMP_TO_EDGE);
						GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
								GLES20.GL_TEXTURE_WRAP_T,
								GLES20.GL_CLAMP_TO_EDGE);
					}

					checkNoGLES2Error();
				}
			});
		}

		@Override
		public void renderFrame(I420Frame frame) {
			if (DEBUG) {
				Log.d(LOG_TAG, "VideoStreamRenderer::renderFrame: frame="
						+ frame);
			}

			final I420Frame frameCopy = takeFrame(frame);
			if (frameCopy == null) {
				return;
			}
			frameCopy.copyFrom(frame);

			boolean needToScheduleRender;
			synchronized (mFramesToRender) {
				// A new render needs to be scheduled (via updateFrames()) iff
				// there isn't
				// already a render scheduled, which is true iff mFramesToRender
				// is empty.
				needToScheduleRender = mFramesToRender.isEmpty();
				I420Frame frameToDrop = mFramesToRender.put(mIndex, frameCopy);
				if (frameToDrop != null) {
					returnFrame(frameToDrop);
				}
			}
			if (needToScheduleRender) {
				queueEvent(new Runnable() {
					public void run() {
						updateFrames();
					}
				});
			}
		}

		private I420Frame takeFrame(I420Frame source) {
			if (DEBUG) {
				Log.d(LOG_TAG, "VideoStreamRenderer::takeFrame source="
						+ source);
			}

			synchronized (mFramePool) {
				while (!mFramePool.isEmpty()) {
					I420Frame frame = mFramePool.pop();
					if (frame.width == source.width
							&& frame.height == source.height
							&& Arrays.equals(frame.yuvStrides,
									source.yuvStrides)) {

						return frame;
					}

					mAllocatedFrames--;
				}
			}

			if (mAllocatedFrames > MAX_ALLOCATED_FRAMES) {
				return null;
			}

			I420Frame frame = new I420Frame(source.width, source.height,
					source.yuvStrides, null);

			mAllocatedFrames++;

			return frame;
		}

		private void returnFrame(I420Frame frame) {
			if (DEBUG) {
				Log.d(LOG_TAG, "VideoStreamRenderer::returnFrame frame="
						+ frame);
			}

			synchronized (mFramePool) {
				mFramePool.add(frame);
			}
		}

		private void setSize() {
			if (DEBUG) {
				Log.d(LOG_TAG, "VideoStreamRenderer::setSize");
			}

			int orientation = mOrientation.ordinal();
			int index = mIndex.ordinal();

			if (mWidth == 0 || mHeight == 0) {
				return;
			}

			float rectangleWidth = (mWidth * sRectangles[orientation][index][WIDTH]) / 2f;
			float rectangleHeight = (mHeight * sRectangles[orientation][index][HEIGHT]) / 2f;

			float widthRatio;
			float heightRatio;
			if (mFrameWidth * rectangleHeight < mFrameHeight * rectangleWidth) {
				widthRatio = ((float) (mFrameWidth * rectangleHeight) / (float) (rectangleWidth * mFrameHeight)) / 2f;
				heightRatio = 0.5f;
			} else {
				widthRatio = 0.5f;
				heightRatio = ((float) (rectangleWidth * mFrameHeight) / (float) (mFrameWidth * rectangleHeight)) / 2f;
			}

			float[] rectangles = new float[8];
			rectangles[0] = sRectangles[orientation][index][CENTER_X]
					- (sRectangles[orientation][index][WIDTH] * widthRatio);
			rectangles[1] = sRectangles[orientation][index][CENTER_Y]
					+ (sRectangles[orientation][index][HEIGHT] * heightRatio);
			rectangles[2] = sRectangles[orientation][index][CENTER_X]
					- (sRectangles[orientation][index][WIDTH] * widthRatio);
			rectangles[3] = sRectangles[orientation][index][CENTER_Y]
					- (sRectangles[orientation][index][HEIGHT] * heightRatio);
			rectangles[4] = sRectangles[orientation][index][CENTER_X]
					+ (sRectangles[orientation][index][WIDTH] * widthRatio);
			rectangles[5] = sRectangles[orientation][index][CENTER_Y]
					+ (sRectangles[orientation][index][HEIGHT] * heightRatio);
			rectangles[6] = sRectangles[orientation][index][CENTER_X]
					+ (sRectangles[orientation][index][WIDTH] * widthRatio);
			rectangles[7] = sRectangles[orientation][index][CENTER_Y]
					- (sRectangles[orientation][index][HEIGHT] * heightRatio);

			mVertices[orientation][index] = directNativeFloatBuffer(rectangles);
		}

	}

	// Rectangle coordinates: Center x, Center y, width, height
	private static final int CENTER_X = 0;
	private static final int CENTER_Y = 1;
	private static final int WIDTH = 2;
	private static final int HEIGHT = 3;

	private final static float[][][] sRectangles = {
			{ { 0.25f, 0f, 1.5f, 2f }, { -0.75f, 0f, 0.5f, 1f },
					{ -0.75f, 0.75f, 0.5f, 1f }, { -0.75f, -0.75f, 0.5f, 1f } },
			{ { 0f, 0.25f, 2f, 1.5f }, { 0f, -0.75f, 1f, 0.5f },
					{ -0.75f, -0.75f, 1f, 0.5f }, { 0.75f, -0.75f, 1f, 0.5f } } };

	private Activity mActivity;

	private Orientation mOrientation;

	private int mWidth;
	private int mHeight;

	private VideoRendererIndex[] mLocalVideoRendererIndexes = {
			VideoRendererIndex.THUMBNAIL_VIDEO_0,
			VideoRendererIndex.THUMBNAIL_VIDEO_1,
			VideoRendererIndex.THUMBNAIL_VIDEO_2 };
	private VideoRendererIndex[] mRemoteVideoRendererIndexes = {
			VideoRendererIndex.MAIN_VIDEO,
			VideoRendererIndex.THUMBNAIL_VIDEO_1,
			VideoRendererIndex.THUMBNAIL_VIDEO_2 };

	private VideoStreamRenderer[] mVideoStreamRenderers = new VideoStreamRenderer[VideoRendererIndex
			.values().length];

	private FloatBuffer[][] mVertices = new FloatBuffer[Orientation.values().length][VideoRendererIndex
			.values().length];

	private int[][] mTextures = { { -1, -1, -1 }, { -1, -1, -1 },
			{ -1, -1, -1 }, { -1, -1, -1 } };

	private EnumMap<VideoRendererIndex, I420Frame> mFramesToRender = new EnumMap<VideoRendererIndex, I420Frame>(
			VideoRendererIndex.class);

	private int mPosLocation = -1;
	private long mRenderTimeStamp = SystemClock.elapsedRealtime();

	private long mLastFPSLogTime = System.nanoTime();
	private long MNumFramesSinceLastLog = 0;

	// Never called
	public VideoStreamsView(Context context) {
		super(context);
	}

	public VideoStreamsView(Activity activity) {
		super(activity);

		mActivity = activity;

		Point displaySize = new Point();
		activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

		mWidth = displaySize.x;
		mHeight = displaySize.y;

		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(RENDERMODE_WHEN_DIRTY);

		Configuration configuration = getResources().getConfiguration();
		if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mOrientation = Orientation.LANDSCAPE;
		} else {
			mOrientation = Orientation.PORTRAIT;
		}

		for (int i = 0; i < Orientation.values().length; i++) {
			for (int j = 0; j < VideoRendererIndex.values().length; j++) {
				float[] rectangles = new float[8];
				rectangles[0] = sRectangles[i][j][CENTER_X]
						- (sRectangles[i][j][WIDTH] / 2f);
				rectangles[1] = sRectangles[i][j][CENTER_Y]
						+ (sRectangles[i][j][HEIGHT] / 2f);
				rectangles[2] = sRectangles[i][j][CENTER_X]
						- (sRectangles[i][j][WIDTH] / 2f);
				rectangles[3] = sRectangles[i][j][CENTER_Y]
						- (sRectangles[i][j][HEIGHT] / 2f);
				rectangles[4] = sRectangles[i][j][CENTER_X]
						+ (sRectangles[i][j][WIDTH] / 2f);
				rectangles[5] = sRectangles[i][j][CENTER_Y]
						+ (sRectangles[i][j][HEIGHT] / 2f);
				rectangles[6] = sRectangles[i][j][CENTER_X]
						+ (sRectangles[i][j][WIDTH] / 2f);
				rectangles[7] = sRectangles[i][j][CENTER_Y]
						- (sRectangles[i][j][HEIGHT] / 2f);
				mVertices[i][j] = directNativeFloatBuffer(sRectangles[i][j]);
			}
		}

		synchronized (this) {
			for (int i = 0; i < mVideoStreamRenderers.length; i++) {
				mVideoStreamRenderers[i] = null;
			}
		}
	}

	public VideoRenderer getLocalVideoRenderer() {
		synchronized (this) {
			for (int i = 0; i < mLocalVideoRendererIndexes.length; i++) {
				VideoRendererIndex localVideoRendererIndex = mLocalVideoRendererIndexes[i];
				int videoRendererIndex = localVideoRendererIndex.ordinal();
				if (mVideoStreamRenderers[videoRendererIndex] == null) {
					VideoStreamRenderer videoStreamRenderer = new VideoStreamRenderer(
							localVideoRendererIndex);
					mVideoStreamRenderers[videoRendererIndex] = videoStreamRenderer;

					return videoStreamRenderer.getVideoRenderer();
				}
			}
		}

		return null;
	}

	public VideoRenderer getRemoteVideoRenderer() {
		synchronized (this) {
			for (int i = 0; i < mRemoteVideoRendererIndexes.length; i++) {
				VideoRendererIndex remoteVideoRendererIndex = mRemoteVideoRendererIndexes[i];
				int videoRendererIndex = remoteVideoRendererIndex.ordinal();
				if (mVideoStreamRenderers[videoRendererIndex] == null) {
					VideoStreamRenderer videoStreamRenderer = new VideoStreamRenderer(
							remoteVideoRendererIndex);
					mVideoStreamRenderers[videoRendererIndex] = videoStreamRenderer;

					return videoStreamRenderer.getVideoRenderer();
				}
			}
		}
		return null;
	}

	public void disposeVideoRenderer(VideoRenderer videoRenderer) {
		synchronized (this) {
			for (int i = 0; i < mVideoStreamRenderers.length; i++) {
				if (mVideoStreamRenderers[i] != null
						&& mVideoStreamRenderers[i].getVideoRenderer() == videoRenderer) {
					mVideoStreamRenderers[i] = null;

					return;
				}
			}
		}
	}

	public void disposeVideoRenderers() {
		synchronized (this) {
			for (int i = 0; i < mVideoStreamRenderers.length; i++) {
				if (mVideoStreamRenderers[i] != null) {
					mVideoStreamRenderers[i] = null;
				}
			}
		}
	}

	@Override
	protected void onMeasure(int unusedX, int unusedY) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onMeasure: unusedX=" + unusedX + " unusedY="
					+ unusedY);
		}

		setMeasuredDimension(mWidth, mHeight);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onSurfaceChanged: width=" + width + " height="
					+ height);
		}

		mWidth = width;
		mHeight = height;

		GLES20.glViewport(0, 0, mWidth, mHeight);

		checkNoGLES2Error();
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onDrawFrame");
		}

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		int orientation = mOrientation.ordinal();
		for (int i = 0; i < VideoRendererIndex.values().length; i++) {
			if (mTextures[i][0] != -1 && mTextures[i][1] != -1
					&& mTextures[i][2] != -1) {
				drawRectangle(mTextures[i], mVertices[orientation][i]);
			}
		}

		if (DEBUG) {
			++MNumFramesSinceLastLog;
			long now = System.nanoTime();
			if (mLastFPSLogTime == -1 || now - mLastFPSLogTime > 1e9) {
				double fps = MNumFramesSinceLastLog
						/ ((now - mLastFPSLogTime) / 1e9);

				Log.d(LOG_TAG, "Rendered FPS: " + fps);

				mLastFPSLogTime = now;
				MNumFramesSinceLastLog = 1;
			}
		}

		checkNoGLES2Error();
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onSurfaceCreated");
		}

		int program = GLES20.glCreateProgram();
		addShaderTo(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_STRING, program);
		addShaderTo(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_STRING, program);

		GLES20.glLinkProgram(program);
		int[] result = new int[] { GLES20.GL_FALSE };
		result[0] = GLES20.GL_FALSE;
		GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, result, 0);
		abortUnless(result[0] == GLES20.GL_TRUE,
				GLES20.glGetProgramInfoLog(program));
		GLES20.glUseProgram(program);

		GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "y_tex"), 0);
		GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_tex"), 1);
		GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "v_tex"), 2);

		// Actually set in drawRectangle(), but queried only once here.
		mPosLocation = GLES20.glGetAttribLocation(program, "in_pos");

		int tcLocation = GLES20.glGetAttribLocation(program, "in_tc");
		GLES20.glEnableVertexAttribArray(tcLocation);
		GLES20.glVertexAttribPointer(tcLocation, 2, GLES20.GL_FLOAT, false, 0,
				textureCoords);

		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		checkNoGLES2Error();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		if (DEBUG) {
			Log.d(LOG_TAG, "onConfigurationChanged newConfig=" + newConfig);
		}

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mOrientation = Orientation.LANDSCAPE;
		} else {
			mOrientation = Orientation.PORTRAIT;
		}

		Point displaySize = new Point();
		mActivity.getWindowManager().getDefaultDisplay().getSize(displaySize);

		mWidth = displaySize.x;
		mHeight = displaySize.y;

		synchronized (this) {
			for (int i = 0; i < mVideoStreamRenderers.length; i++) {
				if (mVideoStreamRenderers[i] != null) {
					mVideoStreamRenderers[i].setSize();
				}
			}
		}
	}

	/**
	 * Upload the planes from |frameToRender| to the textures owned by this
	 * View.
	 */
	private void updateFrames() {
		if (DEBUG) {
			Log.d(LOG_TAG, "updateFrames");
		}

		I420Frame[] mI420Frames = new I420Frame[VideoRendererIndex.values().length];
		synchronized (mFramesToRender) {
			for (int i = 0; i < VideoRendererIndex.values().length; i++) {
				mI420Frames[i] = mFramesToRender.remove(VideoRendererIndex
						.values()[i]);
			}
		}

		for (int i = 0; i < VideoRendererIndex.values().length; i++) {
			I420Frame i420Frame = mI420Frames[i];
			if (i420Frame == null) {
				continue;
			}

			int[] textures = mTextures[i];
			texImage2D(i420Frame, textures);
			VideoStreamRenderer videoStreamRenderer = null;
			synchronized (this) {
				videoStreamRenderer = mVideoStreamRenderers[i];
			}
			if (videoStreamRenderer != null) {
				videoStreamRenderer.returnFrame(i420Frame);
			}
		}

		long elapsedTime = SystemClock.elapsedRealtime();
		if (elapsedTime - mRenderTimeStamp >= MAX_FPS) {
			mRenderTimeStamp = elapsedTime;

			requestRender();
		}
	}

	// Upload the YUV planes from |frame| to |textures|.
	private void texImage2D(I420Frame frame, int[] textures) {
		if (DEBUG) {
			Log.d(LOG_TAG, "texImage2D frame=" + frame + " textures="
					+ textures);
		}

		for (int i = 0; i < 3; ++i) {
			ByteBuffer plane = frame.yuvPlanes[i];
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
			int width = i == 0 ? frame.width : frame.width / 2;
			int height = i == 0 ? frame.height : frame.height / 2;
			if (width != frame.yuvStrides[i]) {
				break;
			}

			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
					width, height, 0, GLES20.GL_LUMINANCE,
					GLES20.GL_UNSIGNED_BYTE, plane);
		}

		checkNoGLES2Error();
	}

	// Wrap a float[] in a direct FloatBuffer using native byte order.
	private static FloatBuffer directNativeFloatBuffer(float[] array) {
		if (DEBUG) {
			Log.d(LOG_TAG, "directNativeFloatBuffer");
		}

		FloatBuffer buffer = ByteBuffer.allocateDirect(array.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		buffer.put(array);
		buffer.flip();
		return buffer;
	}

	// Compile & attach a |type| shader specified by |source| to |program|.
	private static void addShaderTo(int type, String source, int program) {
		if (DEBUG) {
			Log.d(LOG_TAG, "addShaderTo");
		}

		int[] result = new int[] { GLES20.GL_FALSE };
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, source);
		GLES20.glCompileShader(shader);
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, result, 0);
		abortUnless(result[0] == GLES20.GL_TRUE,
				GLES20.glGetShaderInfoLog(shader) + ", source: " + source);
		GLES20.glAttachShader(program, shader);
		GLES20.glDeleteShader(shader);
		checkNoGLES2Error();
	}

	// Poor-man's assert(): die with |msg| unless |condition| is true.
	private static void abortUnless(boolean condition, String msg) {
		if (!condition) {
			throw new RuntimeException(msg);
		}
	}

	// Assert that no OpenGL ES 2.0 error has been raised.
	private static void checkNoGLES2Error() {
		int error = GLES20.glGetError();
		abortUnless(error == GLES20.GL_NO_ERROR, "GLES20 error: " + error);
	}

	// Texture Coordinates mapping the entire texture.
	private static final FloatBuffer textureCoords = directNativeFloatBuffer(new float[] {
			0, 0, 0, 1, 1, 0, 1, 1 });

	// Pass-through vertex shader.
	private static final String VERTEX_SHADER_STRING = "varying vec2 interp_tc;\n"
			+ "\n"
			+ "attribute vec4 in_pos;\n"
			+ "attribute vec2 in_tc;\n"
			+ "\n"
			+ "void main() {\n"
			+ "  gl_Position = in_pos;\n"
			+ "  interp_tc = in_tc;\n" + "}\n";

	// YUV to RGB pixel shader. Loads a pixel from each plane and pass through
	// the
	// matrix.
	private static final String FRAGMENT_SHADER_STRING = "precision mediump float;\n"
			+ "varying vec2 interp_tc;\n"
			+ "\n"
			+ "uniform sampler2D y_tex;\n"
			+ "uniform sampler2D u_tex;\n"
			+ "uniform sampler2D v_tex;\n"
			+ "\n"
			+ "void main() {\n"
			+ "  float y = texture2D(y_tex, interp_tc).r;\n"
			+ "  float u = texture2D(u_tex, interp_tc).r - .5;\n"
			+ "  float v = texture2D(v_tex, interp_tc).r - .5;\n"
			+
			// CSC according to http://www.fourcc.org/fccyvrgb.php
			"  gl_FragColor = vec4(y + 1.403 * v, "
			+ "                      y - 0.344 * u - 0.714 * v, "
			+ "                      y + 1.77 * u, 1);\n" + "}\n";

	private void drawRectangle(int[] textures, FloatBuffer vertices) {
		if (DEBUG) {
			Log.d(LOG_TAG, "drawRectangle textures=" + textures + " vertices="
					+ vertices);
		}

		for (int i = 0; i < 3; ++i) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
		}

		GLES20.glVertexAttribPointer(mPosLocation, 2, GLES20.GL_FLOAT, false,
				0, vertices);
		GLES20.glEnableVertexAttribArray(mPosLocation);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		checkNoGLES2Error();
	}
}