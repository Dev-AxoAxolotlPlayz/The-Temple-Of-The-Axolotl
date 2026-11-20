package com.devaxo.templeoftheaxolotl.promise;
import net.minecraft.world.phys.Vec3;
/** Length-preserving positional FABRIK. Local Euler constraints are applied by the pose adapter. */
public final class PromiseFabrik {
    private PromiseFabrik(){}
    public static Vec3[] solve(Vec3[] initial, Vec3 target, int iterations) {
        Vec3[] p=initial.clone(); if(p.length<2)return p;
        double[] length=new double[p.length-1]; double total=0;
        for(int i=0;i<length.length;i++){length[i]=p[i].distanceTo(p[i+1]);total+=length[i];}
        Vec3 root=p[0]; int last=p.length-1;
        if(root.distanceTo(target)>=total){Vec3 d=target.subtract(root).normalize();for(int i=1;i<p.length;i++)p[i]=p[i-1].add(d.scale(length[i-1]));return p;}
        for(int k=0;k<Math.min(32,iterations);k++){
            p[last]=target;
            for(int i=last-1;i>=0;i--)p[i]=p[i+1].add(p[i].subtract(p[i+1]).normalize().scale(length[i]));
            p[0]=root;
            for(int i=1;i<=last;i++)p[i]=p[i-1].add(p[i].subtract(p[i-1]).normalize().scale(length[i-1]));
            if(p[last].distanceToSqr(target)<.0001)break;
        }return p;
    }
}
