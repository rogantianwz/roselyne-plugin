package cn.focus.m.plugin.roselyne.processor;

import cn.focus.m.plugin.roselyne.io.resource.Resource;

import java.util.HashSet;
import java.util.Set;

/**
 * “引用”，只某一个文件中对静态资源的“引用”,这个引用有一个隐含的字段，即它所属的那个文件，抛开文件谈“引用”是毫无意义的。
 * @author rogantian
 * @date 2014-8-21
 * @email rogantianwz@gmail.com
 */
public class Reference {
    
    public class Bound implements Comparable<Bound> {
        
        private int startAtResource;
        
        private int endAtResource;

        public Bound(int startAtResource, int endAtResource) {
            super();
            this.startAtResource = startAtResource;
            this.endAtResource = endAtResource;
        }

        public int getStartAtResource() {
            return startAtResource;
        }

        public void setStartAtResource(int startAtResource) {
            this.startAtResource = startAtResource;
        }

        public int getEndAtResource() {
            return endAtResource;
        }

        public void setEndAtResource(int endAtResource) {
            this.endAtResource = endAtResource;
        }

        @Override
        public String toString() {
            return "Bound [startAtResource=" + startAtResource + ", endAtResource=" + endAtResource + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + endAtResource;
            result = prime * result + startAtResource;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Bound other = (Bound) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (endAtResource != other.endAtResource)
                return false;
            if (startAtResource != other.startAtResource)
                return false;
            return true;
        }

        private Reference getOuterType() {
            return Reference.this;
        }

        public int compareTo(Bound o) {
            if (null == o) {
                return 1;
            }
            
            if (this.startAtResource > o.getStartAtResource()) {
                return 1;
            } else if (this.startAtResource == o.getStartAtResource()) {
                return 0;
            } else {
                return -1;
            }
        }
    }
    
    /**
     * 该引用所指向的静态资源的地址，形如<script src="abc.js" />中的abc.js
     */
    private String sourceAddr;

    /**
     * 根据配置文件的配置生成的该引用的“资源”对象，包含了资源的原始地址，输出地址，发布地址等
     */
    private Resource resource;
    
    private boolean temp;
    
    /**
     * 标明该引用是否是一个requesjs的data-main资源
     */
    private boolean requirejsDataMain = false;
    
    private String requiredCfgPathKey;
    
    /**
     * 该引用在所属的文件中所处的位置
     */
    private Set<Bound> bounds;

    public Reference(String sourceAddr, int boundStart, int boundEnd, boolean temp) {
        super();
        this.sourceAddr = sourceAddr;
        this.temp = temp;
        Bound b = new Bound(boundStart, boundEnd);
        if ( null == bounds) {
            bounds = new HashSet<Bound>();
        }
        
        bounds.add(b);
    }
    
    
    /**
     * requirejs配置文件中的资源引用使用到的特定构造函数
     * @param sourceAddr
     * @param requiredCfgPathKey  配置文件的paths中的key
     */
    public Reference(String sourceAddr, String requiredCfgPathKey) {
        super();
        this.sourceAddr = sourceAddr;
        this.requiredCfgPathKey = requiredCfgPathKey;
    }



    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getSourceAddr() {
        return sourceAddr;
    }

    public void setSourceAddr(String sourceAddr) {
        this.sourceAddr = sourceAddr;
    }
    
    public Set<Bound> getBounds() {
        return bounds;
    }

    public void setBounds(Set<Bound> bounds) {
        this.bounds = bounds;
    }
    
    public void addBound(Bound bound) {
        if (null == bounds) {
            bounds = new HashSet<Bound>();
        }
        
        bounds.add(bound);
    }
    
    public void addBounds(Set<Bound> bounds) {
        if (null == this.bounds) {
            this.bounds = new HashSet<Bound>();
        }
        
        this.bounds.addAll(bounds);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceAddr == null) ? 0 : sourceAddr.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Reference other = (Reference) obj;
        if (sourceAddr == null) {
            if (other.sourceAddr != null)
                return false;
        } else if (!sourceAddr.equals(other.sourceAddr))
            return false;
        return true;
    }

    public boolean isTemp() {
        return temp;
    }

    public void setTemp(boolean temp) {
        this.temp = temp;
    }

    public boolean isRequirejsDataMain() {
        return requirejsDataMain;
    }

    public void setRequirejsDataMain(boolean requirejsDataMain) {
        this.requirejsDataMain = requirejsDataMain;
    }

    public String getRequiredCfgPathKey() {
        return requiredCfgPathKey;
    }

    public void setRequiredCfgPathKey(String requiredCfgPathKey) {
        this.requiredCfgPathKey = requiredCfgPathKey;
    }

    
}
