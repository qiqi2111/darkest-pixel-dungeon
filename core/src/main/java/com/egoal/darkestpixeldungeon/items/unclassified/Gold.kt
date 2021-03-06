package com.egoal.darkestpixeldungeon.items.unclassified

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Badges
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.Statistics
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.items.artifacts.GoldPlatedStatue
import com.egoal.darkestpixeldungeon.items.artifacts.MasterThievesArmband
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.watabou.noosa.audio.Sample
import com.watabou.utils.Bundle
import com.watabou.utils.Random
import java.util.ArrayList
import kotlin.math.max

class Gold(value: Int = 1) : Item() {
    init {
        image = ItemSpriteSheet.GOLD
        stackable = true

        quantity = value
    }

    override fun actions(hero: Hero): ArrayList<String> = ArrayList()

    override fun doPickUp(hero: Hero): Boolean {
        var greedyCollect = 0
        hero.buff(GoldPlatedStatue.Greedy::class.java)?.let { greedy ->
            greedyCollect = max(1, greedy.extraCollect(quantity))
        }


        val get = quantity + greedyCollect

        Dungeon.gold += get
        Statistics.GoldCollected += get
        Badges.validateGoldCollected()

        hero.buff(MasterThievesArmband.Thievery::class.java)?.let { thievery ->
            thievery.collect(get)
        }
        GameScene.pickUp(this)

        hero.sprite.showStatus(CharSprite.NEUTRAL, if (greedyCollect == 0) "+$get" else "+$quantity(+$greedyCollect)")
        hero.spendAndNext(TIME_TO_PICK_UP)

        Sample.INSTANCE.play(Assets.SND_GOLD, 1f, 1f, Random.Float(0.9f, 1.1f))

        return true
    }

    override fun isUpgradable(): Boolean = false
    override fun isIdentified(): Boolean = true

    override fun random(): Item = this.apply { quantity = Random.Int(20 + Dungeon.depth * 8, 60 + Dungeon.depth * 16) }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(VALUE, quantity)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        quantity = bundle.getInt(VALUE)
    }

    companion object {
        private const val VALUE = "value"
    }
}