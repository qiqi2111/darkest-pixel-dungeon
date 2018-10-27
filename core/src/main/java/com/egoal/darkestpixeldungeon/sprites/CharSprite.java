/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015  Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2016 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.egoal.darkestpixeldungeon.sprites;

import com.egoal.darkestpixeldungeon.DungeonTilemap;
import com.egoal.darkestpixeldungeon.actors.Char;
import com.egoal.darkestpixeldungeon.actors.Damage;
import com.egoal.darkestpixeldungeon.effects.CriticalShock;
import com.egoal.darkestpixeldungeon.effects.FloatingText;
import com.egoal.darkestpixeldungeon.effects.TorchHalo;
import com.egoal.darkestpixeldungeon.effects.particles.FlameParticle;
import com.egoal.darkestpixeldungeon.effects.particles.SoulFlameParticle;
import com.egoal.darkestpixeldungeon.items.potions.PotionOfInvisibility;
import com.egoal.darkestpixeldungeon.levels.Level;
import com.egoal.darkestpixeldungeon.Assets;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.effects.DarkBlock;
import com.egoal.darkestpixeldungeon.effects.EmoIcon;
import com.egoal.darkestpixeldungeon.effects.IceBlock;
import com.egoal.darkestpixeldungeon.effects.Speck;
import com.egoal.darkestpixeldungeon.effects.Splash;
import com.egoal.darkestpixeldungeon.effects.particles.ShadowParticle;
import com.egoal.darkestpixeldungeon.effects.particles.SnowParticle;
import com.egoal.darkestpixeldungeon.messages.Messages;
import com.egoal.darkestpixeldungeon.scenes.GameScene;
import com.egoal.darkestpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.PosTweener;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class CharSprite extends MovieClip implements Tweener.Listener,
        MovieClip.Listener {

  // Color constants for floating text
  public static final int DEFAULT = 0xFFFFFF;  // 白
  public static final int POSITIVE = 0x00FF00; // 绿
  public static final int NEGATIVE = 0xFF0000;  // 红
  public static final int WARNING = 0xFF8800;  // 橙
  public static final int NEUTRAL = 0xFFFF00; // 中立黄

  private static final float MOVE_INTERVAL = 0.1f;
  private static final float FLASH_INTERVAL = 0.05f;

  public enum State {
    BURNING, LEVITATING, INVISIBLE, PARALYSED, FROZEN, ILLUMINATED, CHILLED,
    DARKENED, MARKED,
    SOUL_BURNING,
  }

  protected Animation idle;
  protected Animation run;
  protected Animation attack;
  protected Animation operate;
  protected Animation zap;
  protected Animation die;

  protected Callback animCallback;

  protected Tweener motion;

  protected Emitter burning;
  protected Emitter chilled;
  protected Emitter marked;
  protected Emitter levitation;

  protected IceBlock iceBlock;
  protected DarkBlock darkBlock;
  protected TorchHalo halo;

  protected EmoIcon emo;

  protected Emitter soulburning_;

  private Tweener jumpTweener;
  private Callback jumpCallback;

  private float flashTime = 0;

  protected boolean sleeping = false;

  public Char ch;

  //used to prevent the actor associated with this sprite from acting until 
  // movement completes
  public volatile boolean isMoving = false;

  public CharSprite() {
    super();
    listener = this;
  }

  public void link(Char ch) {
    this.ch = ch;
    ch.sprite = this;

    place(ch.pos);
    turnTo(ch.pos, Random.Int(Dungeon.level.length()));

    ch.updateSpriteState();
  }

  public PointF worldToCamera(int cell) {

    final int csize = DungeonTilemap.SIZE;

    return new PointF(
            PixelScene.align(Camera.main, ((cell % Dungeon.level.width()) +
                    0.5f) * csize - width * 0.5f),
            PixelScene.align(Camera.main, ((cell / Dungeon.level.width()) +
                    1.0f) * csize - height)
    );
  }

  public void place(int cell) {
    point(worldToCamera(cell));
  }

  public void showStatus(int color, String text, Object... args) {
    if (visible) {
      if (args.length > 0) {
        text = Messages.format(text, args);
      }
      if (ch != null) {
        PointF tile = DungeonTilemap.tileCenterToWorld(ch.pos);
        FloatingText.show(tile.x, tile.y - (width * 0.5f), ch.pos, text, color);
      } else {
        FloatingText.show(x + width * 0.5f, y, text, color);
      }
    }
  }

  public void idle() {
    play(idle);
  }

  public void move(int from, int to) {
    turnTo(from, to);

    play(run);

    motion = new PosTweener(this, worldToCamera(to), MOVE_INTERVAL);
    motion.listener = this;
    parent.add(motion);

    isMoving = true;

    if (visible && Level.water[from] && !ch.flying) {
      GameScene.ripple(from);
    }

  }

  public void interruptMotion() {
    if (motion != null) {
      onComplete(motion);
    }
  }

  public void attack(int cell) {
    turnTo(ch.pos, cell);
    play(attack);
  }

  public void attack(int cell, Callback callback) {
    animCallback = callback;
    turnTo(ch.pos, cell);
    play(attack);
  }

  public void operate(int cell) {
    turnTo(ch.pos, cell);
    play(operate);
  }

  public void zap(int cell) {
    turnTo(ch.pos, cell);
    play(zap);
  }

  public void turnTo(int from, int to) {
    int fx = from % Dungeon.level.width();
    int tx = to % Dungeon.level.width();
    if (tx > fx) {
      flipHorizontal = false;
    } else if (tx < fx) {
      flipHorizontal = true;
    }
  }

  public void jump(int from, int to, Callback callback) {
    jumpCallback = callback;

    int distance = Dungeon.level.distance(from, to);
    jumpTweener = new JumpTweener(this, worldToCamera(to), distance * 4,
            distance * 0.1f);
    jumpTweener.listener = this;
    parent.add(jumpTweener);

    turnTo(from, to);
  }

  public void die() {
    sleeping = false;
    play(die);

    if (emo != null) {
      emo.killAndErase();
    }
  }

  public Emitter emitter() {
    Emitter emitter = GameScene.emitter();
    emitter.pos(this);
    return emitter;
  }

  public Emitter centerEmitter() {
    Emitter emitter = GameScene.emitter();
    emitter.pos(center());
    return emitter;
  }

  public Emitter bottomEmitter() {
    Emitter emitter = GameScene.emitter();
    emitter.pos(x, y + height, width, 0);
    return emitter;
  }

  public void burst(final int color, int n) {
    if (visible) {
      Splash.at(center(), color, n);
    }
  }

  public void bloodBurstA(PointF from, int damage) {
    if (visible) {
      PointF c = center();
      int n = (int) Math.min(9 * Math.sqrt((double) damage / ch.HT), 9);
      Splash.at(c, PointF.angle(from, c), 3.1415926f / 2, blood(), n);
    }
  }

  // critical blood burst
  public void bloodBurstB(PointF from, int damage) {
    if (visible) {
      PointF c = center();
      Splash.at(c, PointF.angle(from, c), 3.1415926f / 2, blood(), 12);
    }
  }

  public void spriteBurst(PointF from, int damage) {
    if (visible) {
      float str = GameMath.clampf(
              (float) Math.sqrt((double) damage / ch.HT) * 1.5f, 1f, 1.5f);
      CriticalShock.show(ch, PointF.angle(from, center()), str);
    }
  }

  public int blood() {
    return 0xFFBB0000;
  }

  public void flash() {
    ra = ba = ga = 1f;
    flashTime = FLASH_INTERVAL;
  }

  public void add(State state) {
    switch (state) {
      case BURNING:
        burning = emitter();
        burning.pour(FlameParticle.FACTORY, 0.06f);
        if (visible) {
          Sample.INSTANCE.play(Assets.SND_BURNING);
        }
        break;
      case LEVITATING:
        levitation = emitter();
        levitation.pour(Speck.factory(Speck.JET), 0.02f);
        break;
      case INVISIBLE:
        PotionOfInvisibility.melt(ch);
        break;
      case PARALYSED:
        paused = true;
        break;
      case FROZEN:
        iceBlock = IceBlock.freeze(this);
        paused = true;
        break;
      case ILLUMINATED:
        GameScene.effect(halo = new TorchHalo(this));
        break;
      case CHILLED:
        chilled = emitter();
        chilled.pour(SnowParticle.FACTORY, 0.1f);
        break;
      case DARKENED:
        darkBlock = DarkBlock.darken(this);
        break;
      case MARKED:
        marked = emitter();
        marked.pour(ShadowParticle.UP, 0.1f);
        break;
      case SOUL_BURNING:
        soulburning_ = emitter();
        soulburning_.pour(SoulFlameParticle.FACTORY, 0.06f);
        if (visible) {
          Sample.INSTANCE.play(Assets.SND_BURNING);
        }
        break;
    }
  }

  public void remove(State state) {
    switch (state) {
      case BURNING:
        if (burning != null) {
          burning.on = false;
          burning = null;
        }
        break;
      case LEVITATING:
        if (levitation != null) {
          levitation.on = false;
          levitation = null;
        }
        break;
      case INVISIBLE:
        alpha(1f);
        break;
      case PARALYSED:
        paused = false;
        break;
      case FROZEN:
        if (iceBlock != null) {
          iceBlock.melt();
          iceBlock = null;
        }
        paused = false;
        break;
      case ILLUMINATED:
        if (halo != null) {
          halo.putOut();
        }
        break;
      case CHILLED:
        if (chilled != null) {
          chilled.on = false;
          chilled = null;
        }
        break;
      case DARKENED:
        if (darkBlock != null) {
          darkBlock.lighten();
          darkBlock = null;
        }
        break;
      case MARKED:
        if (marked != null) {
          marked.on = false;
          marked = null;
        }
        break;
      case SOUL_BURNING:
        if (soulburning_ != null) {
          soulburning_.on = false;
          soulburning_ = null;
        }
        break;
    }
  }

  @Override
  public void update() {

    super.update();

    if (paused && listener != null) {
      listener.onComplete(curAnim);
    }

    if (flashTime > 0 && (flashTime -= Game.elapsed) <= 0) {
      resetColor();
    }

    if (burning != null) {
      burning.visible = visible;
    }
    if (levitation != null) {
      levitation.visible = visible;
    }
    if (iceBlock != null) {
      iceBlock.visible = visible;
    }
    if (chilled != null) {
      chilled.visible = visible;
    }
    if (sleeping) {
      showSleep();
    } else {
      hideSleep();
    }
    if (emo != null) {
      emo.visible = visible;
    }
  }

  public void showSleep() {
    if (emo instanceof EmoIcon.Sleep) {

    } else {
      if (emo != null) {
        emo.killAndErase();
      }
      emo = new EmoIcon.Sleep(this);
      emo.visible = visible;
    }
    idle();
  }

  public void hideSleep() {
    if (emo instanceof EmoIcon.Sleep) {
      emo.killAndErase();
      emo = null;
    }
  }

  public void showAlert() {
    if (emo instanceof EmoIcon.Alert) {

    } else {
      if (emo != null) {
        emo.killAndErase();
      }
      emo = new EmoIcon.Alert(this);
      emo.visible = visible;
    }
  }

  public void hideAlert() {
    if (emo instanceof EmoIcon.Alert) {
      emo.killAndErase();
      emo = null;
    }
  }

  @Override
  public void kill() {
    super.kill();

    if (emo != null) {
      emo.killAndErase();
      emo = null;
    }
  }

  @Override
  public void onComplete(Tweener tweener) {
    if (tweener == jumpTweener) {

      if (visible && Level.water[ch.pos] && !ch.flying) {
        GameScene.ripple(ch.pos);
      }
      if (jumpCallback != null) {
        jumpCallback.call();
      }

    } else if (tweener == motion) {

      synchronized (this) {
        isMoving = false;

        motion.killAndErase();
        motion = null;
        ch.onMotionComplete();

        notifyAll();
      }

    }
  }

  @Override
  public void onComplete(Animation anim) {

    if (animCallback != null) {
      Callback executing = animCallback;
      animCallback = null;
      executing.call();
    } else {

      if (anim == attack) {

        idle();
        ch.onAttackComplete();

      } else if (anim == operate) {

        idle();
        ch.onOperateComplete();

      }

    }
  }

  private static class JumpTweener extends Tweener {

    public Visual visual;

    public PointF start;
    public PointF end;

    public float height;

    public JumpTweener(Visual visual, PointF pos, float height, float time) {
      super(visual, time);

      this.visual = visual;
      start = visual.point();
      end = pos;

      this.height = height;
    }

    @Override
    protected void updateValues(float progress) {
      visual.point(PointF.inter(start, end, progress).offset(0, -height * 4 *
              progress * (1 - progress)));
    }
  }
}
