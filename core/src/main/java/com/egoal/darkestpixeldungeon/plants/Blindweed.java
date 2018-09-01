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
package com.egoal.darkestpixeldungeon.plants;

import com.egoal.darkestpixeldungeon.actors.Char;
import com.egoal.darkestpixeldungeon.actors.buffs.Blindness;
import com.egoal.darkestpixeldungeon.actors.buffs.Buff;
import com.egoal.darkestpixeldungeon.actors.mobs.Mob;
import com.egoal.darkestpixeldungeon.effects.CellEmitter;
import com.egoal.darkestpixeldungeon.effects.Speck;
import com.egoal.darkestpixeldungeon.items.potions.PotionOfInvisibility;
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.actors.Actor;
import com.egoal.darkestpixeldungeon.actors.buffs.Cripple;
import com.watabou.utils.Random;

public class Blindweed extends Plant {

  {
    image = 3;
  }

  @Override
  public void activate() {
    Char ch = Actor.findChar(pos);

    if (ch != null) {
      int len = Random.Int(5, 10);
      Buff.prolong(ch, Blindness.class, len);
      Buff.prolong(ch, Cripple.class, len);
      if (ch instanceof Mob) {
        if (((Mob) ch).state == ((Mob) ch).HUNTING)
          ((Mob) ch).state = ((Mob) ch).WANDERING;
        ((Mob) ch).beckon(Dungeon.level.randomDestination());
      }
    }

    if (Dungeon.visible[pos]) {
      CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 4);
    }
  }

  public static class Seed extends Plant.Seed {
    {
      image = ItemSpriteSheet.SEED_BLINDWEED;

      plantClass = Blindweed.class;
      alchemyClass = PotionOfInvisibility.class;
    }
  }
}
