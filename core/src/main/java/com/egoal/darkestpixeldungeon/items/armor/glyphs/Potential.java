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
package com.egoal.darkestpixeldungeon.items.armor.glyphs;

import com.egoal.darkestpixeldungeon.actors.Damage;
import com.egoal.darkestpixeldungeon.items.armor.Armor;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.actors.Char;
import com.egoal.darkestpixeldungeon.effects.Lightning;
import com.egoal.darkestpixeldungeon.levels.traps.LightningTrap;
import com.egoal.darkestpixeldungeon.sprites.ItemSprite;
import com.egoal.darkestpixeldungeon.sprites.ItemSprite.Glowing;
import com.watabou.noosa.Camera;
import com.watabou.utils.Random;

public class Potential extends Armor.Glyph {

  private static ItemSprite.Glowing WHITE = new ItemSprite.Glowing(0xFFFFFF, 
          0.6f);

  @Override
  public Damage proc(Armor armor, Damage damage) {
    Char attacker = (Char) damage.from;
    Char defender = (Char) damage.to;

    int level = Math.max(0, armor.level());

    if (Random.Int(level + 20) >= 18) {

      int shockDmg = Random.NormalIntRange(defender.HT / 20, defender.HT / 10);

      shockDmg *= Math.pow(0.9, level);

      defender.takeDamage(new Damage(shockDmg, this, defender).addElement
              (Damage.Element.LIGHT));

      checkOwner(defender);
      if (defender == Dungeon.hero) {
        Dungeon.hero.belongings.charge(1f);
        Camera.main.shake(2, 0.3f);
      }

      attacker.sprite.parent.add(new Lightning(attacker.pos, defender.pos, 
              null));

    }

    return damage;
  }

  @Override
  public Glowing glowing() {
    return WHITE;
  }
}
