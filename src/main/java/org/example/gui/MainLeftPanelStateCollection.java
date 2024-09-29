package org.example.gui;


import java.util.ArrayList;

public class MainLeftPanelStateCollection {
    private ArrayList<MainLeftPanelState> st = new ArrayList<>();



    public void setDataBits(int tab, int state){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        st.get(tab).setDataBits(state);
    }
    public void setParityBits(int tab, int state){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        st.get(tab).setParityBit(state);
    }

    public void setStopBits(int tab, int state){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        st.get(tab).setStopBits(state);
    }

    public void setBaudRate(int tab, int state){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        st.get(tab).setBaudRate(state);
    }

    public void setProtocol(int tab, int state){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        st.get(tab).setProtocol(state);
    }

    public int getParityBits(int tab){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        return st.get(tab).getParityBit();
    }

    public int getDataBits(int tab){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        return st.get(tab).getDataBits();
    }

    public int getStopBits(int tab){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        return st.get(tab).getStopBits();
    }

    public int getBaudRate(int tab){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        return st.get(tab).getBaudRate();
    }

    public int getProtocol(int tab){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        return st.get(tab).getProtocol();
    }

    public void addEntry(){
        st.add(new MainLeftPanelState());
    }

    public void removeEntry(int tab){
        if(st.size() <= tab){
            throw new IndexOutOfBoundsException("Для указаной вкладки " + tab + " не найдено в листе параметров, размером " + st.size());
        }
        st.remove(tab);
    }

    public ArrayList<MainLeftPanelState> getAllAsList(){
        return this.st;
    }
}
