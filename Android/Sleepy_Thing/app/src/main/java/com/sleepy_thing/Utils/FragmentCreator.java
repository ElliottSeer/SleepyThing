package com.sleepy_thing.Utils;

import com.sleepy_thing.Base.BaseFragment;
import com.sleepy_thing.Fragment.ChatFragment;
import com.sleepy_thing.Fragment.InfoFragment;
import com.sleepy_thing.Fragment.SetFragment;

import java.util.HashMap;
import java.util.Map;

public class FragmentCreator {
    public static final int INDEX_SET  = 0;
    public static final int INDEX_INFO  = 1;
    public static final int INDEX_CHAT  = 2;
    public static final int PAGE_COUNT  = 3;
    private static Map<Integer, BaseFragment> mViewMap = new HashMap<Integer, BaseFragment>();
    public static BaseFragment getFragment(int index){
        BaseFragment baseFragment = mViewMap.get(index);
        if(baseFragment!=null){
            return baseFragment;
        }
        switch (index){
            case INDEX_SET:
                baseFragment  = new SetFragment();
                break;
            case INDEX_INFO:
                baseFragment = new InfoFragment();
                break;
            case  INDEX_CHAT:
                baseFragment = new ChatFragment();
                break;
        }
        mViewMap.put(index,baseFragment);
        return baseFragment;
    }
}
