package com.egoal.darkestpixeldungeon.levels.diggers;

import com.egoal.darkestpixeldungeon.actors.mobs.npcs.Statuary;
import com.egoal.darkestpixeldungeon.levels.Level;
import com.egoal.darkestpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

/**
 * Created by 93942 on 2018/12/5.
 */

public class StatuaryDigger extends Digger {
  @Override
  public XRect chooseDigArea(XWall wall) {
    return super.chooseCenteredBox(wall, Random.NormalIntRange(2, 5));
  }

  @Override
  public DigResult dig(Level level, XWall wall, XRect rect) {
    Point cen = rect.cen();
    int hs = rect.w() / 2;
    for (Point p : rect.getAllPoints())
      if (Point.DistanceL1(cen, p) <= hs)
        Set(level, p, Terrain.EMPTY);

    int ccen = level.pointToCell(cen);
    for (int i : PathFinder.NEIGHBOURS9)
      Set(level, ccen + i, Terrain.EMPTY_SP);

    Statuary s = new Statuary().random();
    s.pos = ccen;
    level.mobs.add(s);
    
    Point door = rect.cen();
    if (wall.direction == LEFT || wall.direction == RIGHT)
      door.x = wall.x1;
    else
      door.y = wall.y1;
    Set(level, door, Terrain.DOOR);

    DigResult dr = new DigResult();
    dr.type(DigResult.Type.SPECIAL);

    if (-wall.direction != LEFT)
      dr.walls.add(new XWall(rect.x1 - 1, cen.y, LEFT));
    if (-wall.direction != RIGHT)
      dr.walls.add(new XWall(rect.x2 + 1, cen.y, RIGHT));
    if (-wall.direction != UP)
      dr.walls.add(new XWall(cen.x, rect.y1 - 1, UP));
    if (-wall.direction != DOWN)
      dr.walls.add(new XWall(cen.x, rect.y2 + 1, DOWN));

    return dr;
  }
}