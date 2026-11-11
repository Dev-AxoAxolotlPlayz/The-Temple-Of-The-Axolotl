package com.devaxo.templeoftheaxolotl.entity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.List;
/** Loose school cohesion, not tight fish-style formation. */
public class JellyfishGroupGoal extends Goal {
 private final AbyssalJellyfishEntity mob; private AbyssalJellyfishEntity neighbor;
 public JellyfishGroupGoal(AbyssalJellyfishEntity mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
 @Override public boolean canUse(){
  if(!mob.isInWater()||mob.getRandom().nextInt(100)!=0)return false;
  List<AbyssalJellyfishEntity> nearby=mob.level().getEntitiesOfClass(AbyssalJellyfishEntity.class,mob.getBoundingBox().inflate(24),e->e!=mob&&e.isAlive());
  if(nearby.isEmpty())return false;
  neighbor=nearby.stream().min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
  return neighbor!=null&&mob.distanceToSqr(neighbor)>64;
 }
 @Override public boolean canContinueToUse(){return neighbor!=null&&neighbor.isAlive()&&mob.isInWater()&&mob.distanceToSqr(neighbor)>25&&mob.distanceToSqr(neighbor)<1024;}
 @Override public void tick(){if(neighbor!=null)mob.getMoveControl().setWantedPosition(neighbor.getX()+2,neighbor.getY(),neighbor.getZ()+2,0.5);}
 @Override public void stop(){neighbor=null;}
}
