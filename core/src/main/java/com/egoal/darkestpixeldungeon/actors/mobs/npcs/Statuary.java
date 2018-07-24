package com.egoal.darkestpixeldungeon.actors.mobs.npcs;

import com.egoal.darkestpixeldungeon.Assets;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.actors.Actor;
import com.egoal.darkestpixeldungeon.actors.Damage;
import com.egoal.darkestpixeldungeon.actors.buffs.Pressure;
import com.egoal.darkestpixeldungeon.actors.hero.Hero;
import com.egoal.darkestpixeldungeon.actors.mobs.DevilGhost;
import com.egoal.darkestpixeldungeon.actors.mobs.Scorpio;
import com.egoal.darkestpixeldungeon.effects.Speck;
import com.egoal.darkestpixeldungeon.effects.particles.ShadowParticle;
import com.egoal.darkestpixeldungeon.items.Generator;
import com.egoal.darkestpixeldungeon.items.Item;
import com.egoal.darkestpixeldungeon.items.KindOfWeapon;
import com.egoal.darkestpixeldungeon.items.UrnOfShadow;
import com.egoal.darkestpixeldungeon.items.artifacts.ChaliceOfBlood;
import com.egoal.darkestpixeldungeon.items.scrolls.Scroll;
import com.egoal.darkestpixeldungeon.items.weapon.Weapon;
import com.egoal.darkestpixeldungeon.items.weapon.enchantments.Holy;
import com.egoal.darkestpixeldungeon.items.weapon.enchantments.Vampiric;
import com.egoal.darkestpixeldungeon.levels.Level;
import com.egoal.darkestpixeldungeon.messages.Messages;
import com.egoal.darkestpixeldungeon.scenes.GameScene;
import com.egoal.darkestpixeldungeon.scenes.PixelScene;
import com.egoal.darkestpixeldungeon.sprites.CharSprite;
import com.egoal.darkestpixeldungeon.sprites.MobSprite;
import com.egoal.darkestpixeldungeon.ui.RedButton;
import com.egoal.darkestpixeldungeon.ui.RenderedTextMultiline;
import com.egoal.darkestpixeldungeon.ui.Window;
import com.egoal.darkestpixeldungeon.utils.GLog;
import com.egoal.darkestpixeldungeon.windows.IconTitle;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * Created by 93942 on 5/30/2018.
 */

public class Statuary extends NPC{
	
	{
		spriteClass	=	StatuarySprite.class;
	}

	// type things
	//todo: moshen type is needed
	public enum Type{
		ANGEL("angel"), DEVIL("devil"), MONSTER("monster");
		
		public String title;
		Type(String t){
			title	=	t;
		}
	}
	
	private Type type	=	Type.ANGEL;
	
	public Type type(){ return type; }
	public Statuary type(Type t){
		type	=	t;
		name	=	Messages.get(this, "name_"+type.title);
		
		return this;
	}
	public Statuary random(){
		switch(Random.Int(3)){
			case 0:
				type(Type.ANGEL);
				break;
			case 1:
				type(Type.DEVIL);
				break;
			case 2:
				type(Type.MONSTER);
				break;
		}
		
		return this;
	}
	
	@Override
	public CharSprite sprite(){
		StatuarySprite sprite	=	new StatuarySprite();
		sprite.setType(type);
		
		return sprite;
	}
	
	@Override
	public String description(){
		return Messages.get(this, "desc_"+type.title);
	}

	// now interact, 
	private static final float TIME_TO_ANSWER	=	2f;
	private boolean isActive	=	true;
	private int gold	=	0;
	
	@Override
	public boolean interact(){
		if(!isActive)
			return false;

		GameScene.show(new WndStatuary(this));
		
		return false;
	}

	private void onAnswered(boolean agree){
		switch(type){
			case ANGEL:
				isActive	=	!answerAngle(agree, Dungeon.hero);
				break;
			case DEVIL:
				isActive	=	!answerDevil(agree, Dungeon.hero);
				break;
			case MONSTER:
				isActive	=	!answerMonster(agree, Dungeon.hero);
				break;
		}
		// isActive	=	true;
	}
	
	private boolean answerAngle(boolean agree, Hero hero){
		hero.spend(TIME_TO_ANSWER);
		
		if(agree){
			// pray
			if(Random.Int(5)==0){
				// unholy
				hero.takeDamage(new Damage(Random.Int(5, 15), 
					this, hero).type(Damage.Type.MENTAL).addFeature(Damage.Feature.ACCURATE));
				GLog.h(Messages.get(this, "unholy"));
			}else{
				// reward
				int rp	=	(int)(hero.buff(Pressure.class).pressure*0.3f);
				hero.recoverSanity(rp);
				GLog.h(Messages.get(this, "holy"));
				
				if(rp<15){
					hero.SHLD	+=	Random.Int(15, 25);
				}
				
				if(Random.Int(4)==0){
					KindOfWeapon kow	=	hero.belongings.weapon;
					if(kow!=null && kow instanceof Weapon){
						((Weapon)kow).enchant(new Holy());

						GLog.h(Messages.get(this, "infuse"));
					}
					
					// hero.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
				}
			}
		}else {
			// blasphemy
			hero.busy();
			hero.sprite.operate(hero.pos);
			GLog.i(Messages.get(this, "blasphemy"));
			
			
		}
		
		return true;
	}
	private boolean answerDevil(boolean agree, Hero hero){
		hero.spend(TIME_TO_ANSWER);
		
		if(agree){
			// sacrifice
			if(hero.HP<hero.HT/2){
				GLog.i(Messages.get(this, "lowhp"));
				return false;
			}
			
			hero.takeDamage(new Damage(hero.HT/5, this, hero));
			hero.busy();
			hero.sprite.operate(hero.pos);
			GLog.h(Messages.get(this, "sacrifice"));
			
			if(Random.Int(10)==0){
				// nothing happened
				GLog.i(Messages.get(this, "nothing"));
			}else{
				// deserve & reward
				boolean requireBlood	=	Random.Float()<.75f;
				int requireValue	=	0;
				if(requireBlood){
					// more blood
					requireValue	=	(int)(hero.HT*Random.Float(.4f,.7f));
					if(requireValue>=hero.HP)
						requireValue	=	hero.HP-1;

					hero.takeDamage(new Damage(requireValue,this,hero).addFeature(Damage.Feature.PURE));
					GLog.h(Messages.get(this, "more_blood"));
				}else{
					// sanity
					requireValue	=	Random.Int(18, 30);
					hero.takeDamage(new Damage(requireValue, this, hero).type(
						Damage.Type.MENTAL).addFeature(Damage.Feature.PURE));
					GLog.h(Messages.get(this, "more_sanity"));
				}
				
				// reward
				UrnOfShadow uos	=	hero.belongings.getItem(UrnOfShadow.class);
				if(uos!=null && uos.level()<10){
					uos.upgrade();
				}else{
					// must be positive
					if(requireBlood){
						hero.HT	+=	Random.Int(5,10);
						GLog.i(Messages.get(this, "upht"));
						if(Random.Int(5)==0){
							KindOfWeapon kow	=	hero.belongings.weapon;
							if(kow!=null && kow instanceof Weapon){
								((Weapon)kow).enchant(new Vampiric());

								GLog.h(Messages.get(this, "infuse"));
							}
						}
					}else{
						// rare
						if(Dungeon.limitedDrops.chaliceOfBlood.count==0 && Random.Int(4)==0){
							Dungeon.limitedDrops.chaliceOfBlood.count	=	1;
							
							Dungeon.level.drop(new ChaliceOfBlood().identify(), hero.pos).sprite.drop();
							hero.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10);
							Sample.INSTANCE.play(Assets.SND_BURNING);
							
						}else{
							hero.earnExp(hero.maxExp());
							GLog.i(Messages.get(this, "upexp"));
						}
					}
				}
			}
			
		}else{
			// blasphemy
			hero.busy();
			hero.sprite.operate(hero.pos);
			GLog.i(Messages.get(this, "blasphemy"));
			
			// spawn ghost
			int count	=	Random.Int(10)==0? 2: 1;
			for(int n: PathFinder.NEIGHBOURS4){
				int cell	=	pos+n;
				if(Level.passable[cell] && Actor.findChar(cell)==null){
					DevilGhost.spawnAt(cell);
					if(--count==0)
						break;
				}
			}
		}
		
		return true;
	}
	private boolean answerMonster(boolean agree, Hero hero){
		if(agree){
			int supply	=	Dungeon.gold>100? 100: Dungeon.gold;
			Dungeon.gold	-=	supply;
			Sample.INSTANCE.play(Assets.SND_GOLD);
			GLog.i(Messages.get(this, "supply", supply));
			
			gold	+=	supply;
			if(supply<100){
				GLog.i(Messages.get(this, "nothing", supply));
			}else{
				// give reward, random things
				if(Random.Int(500)<gold){
					Dungeon.level.drop(Generator.random(), hero.pos);
				}
			}
			
			return gold>=500;
		}else{
			// blasphemy, reset gold
			gold	=	0;
			hero.busy();
			hero.sprite.operate(hero.pos);
			GLog.i(Messages.get(this, "blasphemy"));
		}
		
		return true;
	}
	
	//
	private static final String TYPE	=	"TYPE";
	private static final String ACTIVE	=	"ACTIVE";
	private static final String GOLD	=	"GOLD";
	
	@Override
	public void storeInBundle(Bundle bundle){
		super.storeInBundle(bundle);
		bundle.put(TYPE, type.toString());
		bundle.put(ACTIVE, isActive);
		bundle.put(GOLD, gold);
	}
	@Override
	public void restoreFromBundle(Bundle bundle){
		super.restoreFromBundle(bundle);
		String value	=	bundle.getString(TYPE);
		type(value.length()>0? Type.valueOf(value): Type.ANGEL);
		isActive	=	bundle.getBoolean(ACTIVE);
		gold	=	bundle.getInt(GOLD);
	}
	
	// sprite class
	public static class StatuarySprite extends MobSprite{
		
		Animation idleAngel;
		Animation idleDevil;
		Animation idleMonster;
		
		public StatuarySprite(){
			super();

			texture(Assets.DPD_STATUARY);

			TextureFilm frames	=	new TextureFilm(texture,14,16);

			idle	=	new Animation(10,true);
			
			idle.frames(frames,0);

			run	=	new Animation(20,true);
			run.frames(frames,0);

			die	=	new Animation(20,false);
			die.frames(frames,0);

			play(idle);
			
			// 
			idleAngel	=	new Animation(10, true);
			idleAngel.frames(frames, 0);
			
			idleDevil	=	new Animation(10, true);
			idleDevil.frames(frames, 1);
			
			idleMonster	=	new Animation(10, true);
			idleMonster.frames(frames, 2);
		}
		
		public StatuarySprite setType(Type t){
			switch(t){
				case ANGEL:
					play(idleAngel);
					break;
				case DEVIL:
					play(idleDevil);
					break;
				case MONSTER:
					play(idleMonster);
					break;
			}
			
			return this;
		}
	}
	
	// reaction
	public static class WndStatuary extends Window{

		private static final int WIDTH  =   120;
		private static final float GAP  =   2.f;
		private static final int BTN_HEIGHT =   20;

		private static final String TXT_AGREE   =   "agree_";
		private static final String TXT_DISAGREE    =   "disagree_";
		
		public WndStatuary(final Statuary s){
			super();

			IconTitle titleBar	=	new IconTitle();
			titleBar.icon(s.sprite());
			titleBar.label(s.name);
			titleBar.setRect(0, 0, WIDTH, 0);
			add(titleBar);

			RenderedTextMultiline rtmMessage	=	PixelScene.renderMultiline(s.description(), 6);
			rtmMessage.maxWidth(WIDTH);
			rtmMessage.setPos(0f, titleBar.bottom()+GAP);
			add(rtmMessage);
			
			// buttons
			RedButton btnAgree	=	new RedButton(Messages.get(Statuary.class, TXT_AGREE+s.type().title)){
				@Override
				protected void onClick(){
					hide();
					s.onAnswered(true);
				}
			};
			btnAgree.setRect(0, rtmMessage.bottom()+GAP, WIDTH, BTN_HEIGHT);
			add(btnAgree);
			
			RedButton btnDisagree	=	new RedButton(Messages.get(Statuary.class, TXT_DISAGREE+s.type().title)){
				@Override
				protected void onClick(){
					hide();
					s.onAnswered(false);
				}
			};
			btnDisagree.setRect(0, btnAgree.bottom()+GAP, WIDTH, BTN_HEIGHT);
			add(btnDisagree);
			
			resize(WIDTH, (int)btnDisagree.bottom());
		}
		
	}
}
