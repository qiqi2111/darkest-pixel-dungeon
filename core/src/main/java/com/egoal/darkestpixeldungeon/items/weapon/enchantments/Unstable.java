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
package com.egoal.darkestpixeldungeon.items.weapon.enchantments;

import com.egoal.darkestpixeldungeon.DarkestPixelDungeon;
import com.egoal.darkestpixeldungeon.actors.Char;
import com.egoal.darkestpixeldungeon.actors.Damage;
import com.egoal.darkestpixeldungeon.items.weapon.Weapon;
import com.egoal.darkestpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

public class Unstable extends Weapon.Enchantment {

  private static ItemSprite.Glowing WHITE = new ItemSprite.Glowing(0xFFFFFF);

  private static Class<? extends Weapon.Enchantment>[] randomEnchants = new 
          Class[]{
          Blazing.class,
          Chilling.class,
          Dazzling.class,
          Eldritch.class,
          Grim.class,
          Lucky.class,
          Shocking.class,
          Stunning.class,
          Vampiric.class,
          Vorpal.class
  };

  @Override
  public Damage proc(Weapon weapon, Damage damage) {
    try {
      return Random.oneOf(randomEnchants).newInstance().proc(weapon, damage);
    } catch (Exception e) {
      DarkestPixelDungeon.reportException(e);
      return damage;
    }
  }

  @Override
  public ItemSprite.Glowing glowing() {
    return WHITE;
  }
}
