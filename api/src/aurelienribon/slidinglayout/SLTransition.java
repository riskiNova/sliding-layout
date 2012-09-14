package aurelienribon.slidinglayout;

import aurelienribon.slidinglayout.SLConfig.Tile;
import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenManager;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * A transition consists of one or more keyframes (ie. configurations). The
 * engine will move the children of the panel from the current configuration to
 * the first keyframe, and so on.
 * <p/>
 *
 * If a component is present on a keyframe but absent from the current
 * configuration, it will be considered to be a <b>new component</b>. If you
 * don't specify anything, it will appear right at its target place, which may
 * not be very nice. Instead, you can set its starting side, the side from
 * where it should be brought into view to its target place. It will then slide
 * from this side to its target place.
 * <p/>
 *
 * Similarly, if a component is present in the current configuration but absent
 * from the next keyframe, it will be considered to be an <b>old component</b>.
 * You can slide it out of the screen gracefully by specifying its ending side.
 *
 * @see SLConfig
 * @see SLKeyframe
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class SLTransition {
	private final SLPanel panel;
	private final TweenManager tweenManager;
	private final List<SLKeyframe> keyframes = new ArrayList<SLKeyframe>();
	private int currentKeyframe;
	private Timeline timeline;

	public SLTransition(SLPanel panel, TweenManager tweenManager) {
		this.panel = panel;
		this.tweenManager = tweenManager;
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	/**
	 * Adds a new keyframe to the transition.
	 */
	public SLTransition push(SLKeyframe kf) {
		keyframes.add(kf);
		return this;
	}

	/**
	 * Starts the transition.
	 */
	public SLTransition play() {
		currentKeyframe = 0;
		play(keyframes.get(0), new SLKeyframe(panel.currentCfg, 0));
		return this;
	}

	// -------------------------------------------------------------------------
	// Private API
	// -------------------------------------------------------------------------

	private void play(SLKeyframe kf, SLKeyframe previousKf) {
		panel.currentCfg = kf.getCfg();

		kf.initialize(previousKf);
		tween(kf);
	}

	private void tween(final SLKeyframe kf) {
		if (timeline != null) timeline.kill();

		timeline = Timeline.createParallel();

		for (Component c : kf.getEndCmps()) {
			Tile t = kf.getEndTile(c);

			int dx = c.getX() - t.x;
			int dy = c.getY() - t.y;
			int dw = c.getWidth() - t.w;
			int dh = c.getHeight() - t.h;
			boolean animXY = (dx != 0) || (dy != 0);
			boolean animWH = (dw != 0) || (dh != 0);
			float duration = kf.getDuration();

			if (animXY && animWH) {
				timeline.push(Tween.to(c, SLAnimator.JComponentAccessor.XYWH, duration)
					.target(t.x, t.y, t.w, t.h)
					.delay(kf.getDelay(c))
				);
			} else if (animXY) {
				timeline.push(Tween.to(c, SLAnimator.JComponentAccessor.XY, duration)
					.target(t.x, t.y)
					.delay(kf.getDelay(c))
				);
			} else if (animWH) {
				timeline.push(Tween.to(c, SLAnimator.JComponentAccessor.WH, duration)
					.target(t.w, t.h)
					.delay(kf.getDelay(c))
				);
			}
		}

		timeline.setCallback(new TweenCallback() {
			@Override public void onEvent(int type, BaseTween<?> source) {
				if (kf.getCallback() != null) kf.getCallback().done();
				if (currentKeyframe < keyframes.size()-1) {
					currentKeyframe++;
					play(keyframes.get(currentKeyframe), keyframes.get(currentKeyframe-1));
				}
			}
		});

		timeline.start(tweenManager);
	}
}
