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
package com.egoal.darkestpixeldungeon.items.wands;

import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.DungeonTilemap;
import com.egoal.darkestpixeldungeon.actors.Actor;
import com.egoal.darkestpixeldungeon.actors.Char;
import com.egoal.darkestpixeldungeon.actors.Damage;
import com.egoal.darkestpixeldungeon.effects.Beam;
import com.egoal.darkestpixeldungeon.effects.CellEmitter;
import com.egoal.darkestpixeldungeon.effects.particles.PurpleParticle;
import com.egoal.darkestpixeldungeon.items.weapon.melee.MagesStaff;
import com.egoal.darkestpixeldungeon.levels.Level;
import com.egoal.darkestpixeldungeon.mechanics.Ballistica;
import com.egoal.darkestpixeldungeon.scenes.GameScene;
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WandOfDisintegration extends DamageWand {

  {
    image = ItemSpriteSheet.WAND_DISINTEGRATION;

    collisionProperties = Ballistica.WONT_STOP;
  }


  public int min(int lvl) {
    return 2 + lvl;
  }

  public int max(int lvl) {
    return 6 + 4 * lvl;
  }

  @Override
  protected void onZap(Ballistica beam) {

    boolean terrainAffected = false;

    int level = level();

    int maxDistance = Math.min(distance(), beam.dist);

    ArrayList<Char> chars = new ArrayList<>();

    int terrainPassed = 2, terrainBonus = 0;
    for (int c : beam.subPath(1, maxDistance)) {

      Char ch;
      if ((ch = Actor.findChar(c)) != null) {

        //we don't want to count passed terrain after the last enemy hit. 
        // That would be a lot of bonus levels.
        //terrainPassed starts at 2, equivalent of rounding up when /3 for 
        // integer arithmetic.
        terrainBonus += terrainPassed / 3;
        terrainPassed = terrainPassed % 3;

        chars.add(ch);
      }

      if (Level.flamable[c]) {

        Dungeon.level.destroy(c);
        GameScene.updateMap(c);
        terrainAffected = true;

      }

      if (Level.solid[c])
        terrainPassed++;

      CellEmitter.center(c).burst(PurpleParticle.BURST, Random.IntRange(1, 2));
    }

    if (terrainAffected) {
      Dungeon.observe();
    }

    int lvl = level + (chars.size() - 1) + terrainBonus;
    for (Char ch : chars) {
      // ch.damage( damageRoll(lvl), this );
      ch.takeDamage(new Damage(damageRoll(lvl), curUser, ch).type(Damage.Type
              .MAGICAL).addFeature(Damage.Feature.PURE));
      ch.sprite.centerEmitter().burst(PurpleParticle.BURST, Random.IntRange
              (1, 2));
      ch.sprite.flash();
    }
  }

  @Override
  public void onHit(MagesStaff staff, Damage damage) {
    //no direct effect, see magesStaff.reachfactor
  }

  private int distance() {
    return level() * 2 + 4;
  }

  @Override
  public void fx(Ballistica beam, Callback callback) {

    int cell = beam.path.get(Math.min(beam.dist, distance()));
    curUser.sprite.parent.add(new Beam.DeathRay(
            DungeonTilemap.tileCenterToWorld(beam.sourcePos),
            DungeonTilemap.tileCenterToWorld(cell)));
    callback.call();
  }

  @Override
  public void staffFx(MagesStaff.StaffParticle particle) {
    particle.color(0x220022);
    particle.am = 0.6f;
    particle.setLifespan(0.6f);
    particle.acc.set(40, -40);
    particle.setSize(0f, 3f);
    particle.shuffleXY(2f);
  }

}
