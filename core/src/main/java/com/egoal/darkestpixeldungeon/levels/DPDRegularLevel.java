package com.egoal.darkestpixeldungeon.levels;

import android.util.Log;
import android.util.Pair;

import com.egoal.darkestpixeldungeon.Bones;
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.actors.Actor;
import com.egoal.darkestpixeldungeon.actors.mobs.Bestiary;
import com.egoal.darkestpixeldungeon.actors.mobs.Mob;
import com.egoal.darkestpixeldungeon.items.Generator;
import com.egoal.darkestpixeldungeon.items.Heap;
import com.egoal.darkestpixeldungeon.items.Item;
import com.egoal.darkestpixeldungeon.items.rings.RingOfWealth;
import com.egoal.darkestpixeldungeon.items.scrolls.Scroll;
import com.egoal.darkestpixeldungeon.levels.diggers.*;
import com.egoal.darkestpixeldungeon.levels.traps.FireTrap;
import com.egoal.darkestpixeldungeon.levels.traps.Trap;
import com.egoal.darkestpixeldungeon.levels.traps.WornTrap;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by 93942 on 11/11/2018.
 */

public abstract class DPDRegularLevel extends Level {
  {
    color1 = 0x48763c;
    color2 = 0x59994a;
    viewDistance = 8;
    seeDistance = 8;
  }

  @Override
  protected void setupSize() {
    if (width == 0 && height == 0)
      width = height = 36;

    length = width * height;
  }

  @Override
  protected boolean build() {
    // dig rooms
    if (!digLevel())
      return false;

    // place entrance and exit
    Space spaceEntrance = randomSpace(Digger.DigResult.Type.NORMAL, -1);
    do {
      entrance = pointToCell(spaceEntrance.rect.random(1));
    } while (map[entrance] != Terrain.EMPTY);
    spaceEntrance.type = Digger.DigResult.Type.ENTRANCE;

    Space spaceExit;
    do {
      spaceExit = randomSpace(Digger.DigResult.Type.NORMAL, 10);
      exit = pointToCell(spaceExit.rect.random(1));
    } while (map[exit] != Terrain.EMPTY || distance(entrance, exit) < 12);
    spaceExit.type = Digger.DigResult.Type.EXIT;

    map[entrance] = Terrain.ENTRANCE;
    map[exit] = Terrain.EXIT;

    // do some painting
    paintLuminary();
    paintWater();
    paintGrass();
    placeTraps();

    return true;
  }

  @Override
  public int nMobs() {
    switch (Dungeon.depth) {
      case 0:
      case 1:
        return 0;
      default:
        return (3 + Dungeon.depth % 5 + Random.Int(6));
    }
  }

  protected void createSellers() {
  }

  @Override
  protected void createMobs() {
    createSellers();

    int mobsToSpawn = Dungeon.depth == 1 ? 10 : nMobs();

    // well distributed in each rooms
    Iterator<Space> iter = spaces.iterator();
    while (mobsToSpawn > 0) {
      if (!iter.hasNext())
        iter = spaces.iterator();
      Space space = iter.next();
      if (space.type != Digger.DigResult.Type.NORMAL) continue;

      Mob mob = Bestiary.mob(Dungeon.depth);
      mob.pos = pointToCell(space.rect.random());

      if (findMob(mob.pos) == null && passable[mob.pos]) {
        --mobsToSpawn;
        mobs.add(mob);
        if (mobsToSpawn > 0 && Random.Int(4) == 0) {
          mob = Bestiary.mob(Dungeon.depth);
          mob.pos = pointToCell(space.rect.random());
          if (findMob(mob.pos) == null && passable[mob.pos]) {
            --mobsToSpawn;
            mobs.add(mob);
          }
        }
      }

    }
  }

  protected Space randomSpace(Digger.DigResult.Type type, int tries) {
    for (int i = 0; i != tries; ++i) {
      Space space = Random.element(spaces);
      if (space.type == type)
        return space;
    }
    return null;
  }

  @Override
  public int randomRespawnCell() {
    for (int i = 0; i < 30; ++i) {
      Space space = randomSpace(Digger.DigResult.Type.NORMAL, 10);

      int pos = pointToCell(space.rect.random());

      if (!Dungeon.visible[pos] && Actor.findChar(pos) == null && passable[pos])
        return pos;
    }

    return -1;
  }

  @Override
  public int randomDestination() {
    while (true) {
      int pos = Random.Int(length());
      if (passable[pos])
        return pos;
    }
  }

  @Override
  protected void createItems() {
    int nItems = 3;
    {
      // bonus from wealth
      int bonus = RingOfWealth.getBonus(Dungeon.hero,
              RingOfWealth.Wealth.class);
      bonus = Math.min(bonus, 10);
      while (Random.Float() < 0.3f + bonus * 0.05f)
        ++nItems;
    }

    for (int i = 0; i < nItems; ++i) {
      Heap.Type type = null;
      switch (Random.Int(20)) {
        case 0:
          type = Heap.Type.SKELETON;
          break;
        case 1:
        case 2:
        case 3:
        case 4:
          type = Heap.Type.CHEST;
          break;
        case 5:
          type = Dungeon.depth > 1 ? Heap.Type.MIMIC : Heap.Type.CHEST;
          break;
        default:
          type = Heap.Type.HEAP;
      }
      drop(Generator.random(), randomDropCell()).type = type;
    }

    // drop the items
    for (Item item : itemsToSpawn) {
      int cell = randomDropCell();
      // don't drop scroll on fire trap
      if (item instanceof Scroll) {
        while (map[cell] == Terrain.TRAP || map[cell] == Terrain.SECRET_TRAP &&
                traps.get(cell) instanceof FireTrap)
          cell = randomDropCell();
      }

      drop(item, cell).type = Heap.Type.HEAP;
    }

    // drop the hero bones
    Item item = Bones.get();
    if (item != null) {
      drop(item, randomDropCell()).type = Heap.Type.REMAINS;
    }
  }

  protected int randomDropCell() {
    while (true) {
      Space space = randomSpace(Digger.DigResult.Type.NORMAL, 1);
      if (space != null) {
        int cell = pointToCell(space.rect.random());
        if (passable[cell])
          return cell;
      }
    }
  }

  @Override
  public int pitCell() {
    for (Space s : spaces)
      if (s.type == Digger.DigResult.Type.PIT)
        return pointToCell(s.rect.random(1));

    return super.pitCell();
  }

  protected void paintLuminary() {
    // put on wall
    HashSet<Integer> availableWalls = new HashSet<>();
    for (Space s : spaces) {
      for (Point p : s.rect.inner(-1).getAllPoints()) {
        int c = pointToCell(p);
        // check if visible
        if (map[c] == Terrain.WALL) {
          for (int n : PathFinder.NEIGHBOURS4) {
            int np = n + c;
            if (np >= 0 && np < length && (map[np] == Terrain.EMPTY ||
                    map[np] == Terrain.EMPTY_SP ||
                    map[np] == Terrain.EMPTY_DECO)) {
              availableWalls.add(c);
              break;
            }
          }
        }
      }
    }

    final float LIGHT_RATIO = Dungeon.depth < 10 ? 0.15f : 0.1f;
    final float LIGHT_ON_RATIO = feeling == Feeling.DARK ? 0.45f : 0.7f;
    for (int i : availableWalls) {
      if (Random.Float() < LIGHT_RATIO)
        map[i] = Random.Float() < LIGHT_ON_RATIO ?
                Terrain.WALL_LIGHT_ON : Terrain.WALL_LIGHT_OFF;
    }
  }

  protected abstract boolean[] water();

  protected abstract boolean[] grass();

  protected void paintWater() {
    boolean[] waters = water();
    for (int i = 0; i < length(); ++i)
      if (map[i] == Terrain.EMPTY && waters[i])
        map[i] = Terrain.WATER;
  }

  protected void paintGrass() {
    boolean[] grass = grass();

    if (feeling == Feeling.GRASS) {
      for (Space space : spaces) {
        XRect rect = space.rect;
        grass[xy2cell(rect.x1, rect.y1)] = true;
        grass[xy2cell(rect.x2, rect.y1)] = true;
        grass[xy2cell(rect.x1, rect.y2)] = true;
        grass[xy2cell(rect.x2, rect.y2)] = true;
      }
    }

    for (int i = width() + 1; i < length() - width() - 1; i++) {
      if (map[i] == Terrain.EMPTY && grass[i]) {
        int count = 1;
        for (int n : PathFinder.NEIGHBOURS8) {
          if (grass[i + n]) {
            count++;
          }
        }
        map[i] = (Random.Float() < count / 12f) ? Terrain.HIGH_GRASS :
                Terrain.GRASS;
      }
    }
  }

  // traps
  protected int nTraps() {
    return Random.NormalIntRange(1, 3 + Dungeon.depth / 2);
  }

  protected Class<?>[] trapClasses() {
    return new Class<?>[]{WornTrap.class};
  }

  protected float[] trapChances() {
    return new float[]{1};
  }

  protected void placeTraps() {
    float[] trapChances = trapChances();
    Class<?>[] trapClasses = trapClasses();

    LinkedList<Integer> validCells = new LinkedList<Integer>();

    for (int i = 0; i < length(); ++i) {
      if (map[i] == Terrain.EMPTY && findMob(i) == null)
        validCells.add(i);
    }

    int ntraps = Math.min(nTraps(), (int) (validCells.size() * 0.15));
    Collections.shuffle(validCells);

    for (int i = 0; i < ntraps; ++i) {
      int tp = validCells.removeFirst();
      try {
        Trap trap = ((Trap) trapClasses[Random.chances(trapChances)]
                .newInstance()).hide();
        setTrap(trap, tp);
        // some traps would not be hidden
        map[tp] = trap.visible ? Terrain.TRAP : Terrain.SECRET_TRAP;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    Log.d("dpd", String.format("%d traps added.", ntraps));
  }

  //////////////////////////////////////////////////////////////////////////////
  // my digging algorithm
  private ArrayList<XWall> digableWalls; // = new ArrayList<>();

  // space dug, keep in mind that not all type of spaces is rectangle.
  protected ArrayList<Space> spaces;
  private ArrayList<Digger> chosenDiggers = null;

  private boolean digLevel() {
    digableWalls = new ArrayList<>();
    spaces = new ArrayList<>();

    digFirstRoom();

    if (chosenDiggers == null)
      chosenDiggers = chooseDiggers();

    ArrayList<Digger> diggers = (ArrayList<Digger>) chosenDiggers.clone();
    Log.d("dpd", String.format("%d rooms to dig.", diggers.size()));

    while (!diggers.isEmpty() && !digableWalls.isEmpty()) {
      // choose a digger
      Digger digger = Random.element(diggers);

      // dig a room
      boolean digged = false;
      for (int i = 0; i < 100; ++i) {
        XWall wall = Random.element(digableWalls);
        XRect rect = digger.chooseDigArea(wall);
        if (canDigAt(rect)) {
          // free to dig, dig!
          digged = true;

          Digger.DigResult dr = digger.dig(this, wall, rect);

          digableWalls.remove(wall);
          digableWalls.addAll(dr.walls);
          spaces.add(new Space(rect, dr.type));
          break;
        }
      }

      if (!digged) return false;

      diggers.remove(digger);
    }
    Log.d("dpd", String.format("%d diggers left...", diggers.size()));

    int lc = makeLoopClosure(6);
    Log.d("dpd", String.format("%d loop linked.", lc));

    if (lc <= 2)
      return false;

    return true;
  }

  private void digFirstRoom() {
    int w = Random.IntRange(3, 6);
    int h = Random.IntRange(3, 6);
    int x1 = Random.IntRange(width / 4, width / 4 * 3 - w);
    int y1 = Random.IntRange(height / 4, height / 4 * 3 - h);
    int x2 = x1 + w - 1;
    int y2 = y1 + h - 1;

    Digger.Fill(this, x1, y1, w, h, Terrain.EMPTY);
    digableWalls.add(new XWall(x1 - 1, x1 - 1, y1, y2, Digger.LEFT));
    digableWalls.add(new XWall(x2 + 1, x2 + 1, y1, y2, Digger.RIGHT));
    digableWalls.add(new XWall(x1, x2, y1 - 1, y1 - 1, Digger.UP));
    digableWalls.add(new XWall(x1, x2, y2 + 1, y2 + 1, Digger.DOWN));
  }

  // all diggers
  protected static final HashMap<Class<? extends Digger>, Float>
          SPECIAL_DIGGERS = new HashMap<>();
  protected static final HashMap<Class<? extends Digger>, Float>
          NORMAL_DIGGERS = new HashMap<>();

  {
    SPECIAL_DIGGERS.put(ArmoryDigger.class, 1f);
    SPECIAL_DIGGERS.put(GardenDigger.class, 1f);
    SPECIAL_DIGGERS.put(LaboratoryDigger.class, 1f);
    SPECIAL_DIGGERS.put(LibraryDigger.class, 1f);
    SPECIAL_DIGGERS.put(MagicWellDigger.class, 1f);
    SPECIAL_DIGGERS.put(PitDigger.class, 0f);
    SPECIAL_DIGGERS.put(PoolDigger.class, 1f);
    SPECIAL_DIGGERS.put(QuestionerDigger.class, 1f);
    SPECIAL_DIGGERS.put(ShopDigger.class, 0f);
    SPECIAL_DIGGERS.put(StatuaryDigger.class, 1f);
    SPECIAL_DIGGERS.put(StatueDigger.class, 1f);
    SPECIAL_DIGGERS.put(StorageDigger.class, 1f);
    SPECIAL_DIGGERS.put(TrapsDigger.class, 1f);
    SPECIAL_DIGGERS.put(TreasuryDigger.class, 1f);
    SPECIAL_DIGGERS.put(VaultDigger.class, 1f);
    SPECIAL_DIGGERS.put(WeakFloorDigger.class, 1f);

    NORMAL_DIGGERS.put(NormalRoomDigger.class, 1f);
    NORMAL_DIGGERS.put(NormalCircleDigger.class, .2f);
    NORMAL_DIGGERS.put(LatticeDigger.class, .2f);
  }

  protected ArrayList<Digger> chooseDiggers() {
    ArrayList<Digger> diggers = new ArrayList<>();
    int specials = Random.NormalIntRange(4, 8);
    if (Dungeon.shopOnLevel()) {
      diggers.add(new ShopDigger());
      --specials;
    }

    diggers.addAll(selectDiggers(specials, 18));

    return diggers;
  }

  protected ArrayList<Digger> selectDiggers(int specials, int total) {
    ArrayList<Digger> diggers = new ArrayList<>();
    {
      // special diggers, avoid duplicate
      HashMap<Class<? extends Digger>, Float> probs =
              (HashMap<Class<? extends Digger>, Float>) SPECIAL_DIGGERS.clone();

      if (pitRoomNeeded) {
        // a pit digger is need, remove all other iron key diggers
        diggers.add(new PitDigger());
        --specials;

        probs.remove(ArmoryDigger.class);
        probs.remove(LibraryDigger.class);
        probs.remove(StatueDigger.class);
        probs.remove(TreasuryDigger.class);
        probs.remove(VaultDigger.class);
        probs.remove(WeakFloorDigger.class);
      }

      for (int i = 0; i < specials; ++i) {
        Class<? extends Digger> cls = Random.chances(probs);
        if (cls == null) break;

        probs.put(cls, 0f);
        try {
          diggers.add(cls.newInstance());
        } catch (Exception e) {
          DarkestPixelDungeon.reportException(e);
          return null;
        }
      }
    }
    // weak floor
    for (Digger d : diggers)
      if (d instanceof WeakFloorDigger) {
        weakFloorCreated = true;
        break;
      }

    // normal diggers, random draft
    int n = total - diggers.size();
    for (int i = 0; i < n; ++i) {
      Class<? extends Digger> cls = Random.chances(NORMAL_DIGGERS);
      try {
        diggers.add(cls.newInstance());
      } catch (Exception e) {
        DarkestPixelDungeon.reportException(e);
        return null;
      }
    }

    return diggers;
  }

  private boolean canDigAt(XRect rect) {
    if (rect.x1 > 0 && rect.x2 < width - 1 && rect.y1 > 0 && rect.y2 < height
            - 1) {
      for (int x = rect.x1 - 1; x <= rect.x2 + 1; ++x)
        for (int y = rect.y1 - 1; y <= rect.y2 + 1; ++y)
          if (map[xy2cell(x, y)] != Terrain.WALL)
            return false;

      return true;
    }

    return false;
  }

  private int makeLoopClosure(int maxLoops) {
    int loops = 0;

    // simply dig a door when there's overlapped wall
    ArrayList<Pair<XWall, XWall>> overlappedWalls = new ArrayList<>();

    int cntWalls = digableWalls.size();
    for (int i = 0; i < cntWalls; ++i) {
      XWall wi = digableWalls.get(i);
      for (int j = i + 1; j < cntWalls; ++j) {
        XWall wj = digableWalls.get(j);

        if (wi.direction == -wj.direction && wi.overlap(wj).isValid())
          overlappedWalls.add(new Pair<>(wi, wj));
      }
    }

    Collections.shuffle(overlappedWalls);
    for (Pair<XWall, XWall> pr : overlappedWalls) {
      int dp = pointToCell(pr.first.overlap(pr.second).random(0));

      boolean canBeDoor = map[dp] == Terrain.WALL;
      if (canBeDoor) {
        for (int i : PathFinder.NEIGHBOURS8)
          if (map[dp + i] == Terrain.DOOR) {
            canBeDoor = false;
            break;
          }
      }

      if (canBeDoor) {
        Digger.Set(this, dp, Terrain.DOOR);
        digableWalls.remove(pr.first);
        digableWalls.remove(pr.second);

        if (++loops >= maxLoops)
          break;
      }
    }

    //todo: not enough, random select two wall and dig between them
    if (loops < maxLoops && digableWalls.size() >= 2) {
    }

    return loops;
  }

  @Override
  public void storeInBundle(Bundle bundle) {
    super.storeInBundle(bundle);
    bundle.put("spaces", spaces);
  }

  @Override
  public void restoreFromBundle(Bundle bundle) {
    super.restoreFromBundle(bundle);

    spaces = new ArrayList<>((Collection<Space>) (Collection<?>)
            bundle.getCollection("spaces"));
    for (Space s : spaces) {
      //todo: update flags and others...
      if (s.type == Digger.DigResult.Type.WEAK_FLOOR) {
        weakFloorCreated = true;
        break;
      }
    }
    Log.d("dpd", String.format("%d spaces restored.", spaces.size()));
  }

  public static class Space implements Bundlable {
    public XRect rect;
    public Digger.DigResult.Type type;

    // default constructor for deserialization only
    public Space() {
    }

    public Space(XRect rect, Digger.DigResult.Type type) {
      this.rect = rect;
      this.type = type;
    }

    @Override
    public void storeInBundle(Bundle bundle) {
      bundle.put("rect", rect);
      bundle.put("type", type.toString());
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
      rect = (XRect) bundle.get("rect");
      type = Digger.DigResult.Type.valueOf(bundle.getString("type"));
    }
  }
}
