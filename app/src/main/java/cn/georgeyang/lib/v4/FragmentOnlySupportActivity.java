package cn.georgeyang.lib.v4;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.georgeyang.fragment.R;
import cn.georgeyang.lib.OnBackPressedListener;

/**
 * for support v4
 * Created by george.yang on 2016-4-12.
 */
public abstract class FragmentOnlySupportActivity extends FragmentActivity {
    public abstract int getContainerId();
    private int containerId = getContainerId();
    public static boolean fragmentChangeing = false;
    private static final String TAG = "FragmentLoder";

    public <T extends Fragment> T getFragment(@Nullable FragmentManager manager, Class clazz) {
        return getFragment(manager, false, clazz);
    }

    public <T extends Fragment> T getFragment(@Nullable FragmentManager manager, boolean showing, Class clazz) {
        if (clazz == null) {
            return null;
        }
        if (manager == null) {
            manager = getSupportFragmentManager();
        }
        List<Fragment> fragmentlist = manager.getFragments();
        if (fragmentlist != null) {
            for (Fragment fragment : fragmentlist) {
                if (fragment == null || fragment.isRemoving() || !fragment.isAdded() || (showing && fragment.isHidden())) {
                    continue;
                }
                if (clazz.getName().equals(fragment.getClass().getName())) {
                    return (T) fragment;
                } else {
                    Fragment fragmentInner = getFragment(fragment.getChildFragmentManager(), showing, clazz);
                    if (fragmentInner != null) {
                        return (T) fragmentInner;
                    }
                }
            }
        }
        return null;
    }

    public void loadFragment(Fragment tagfragment, FragmentTransaction mFragmentTransaction) {
        loadFragment(tagfragment,mFragmentTransaction, AnimType.LeftInRightOut);
    }

    public void loadFragment(Fragment tagfragment) {
        loadFragment(tagfragment,AnimType.LeftInRightOut);
    }

    public void loadFragment(Fragment tagfragment,@NonNull AnimType animType) {
        loadFragment(tagfragment, null, animType);
    }

    private int fragmentIndex;
    public void loadFragment(Fragment tagfragment, FragmentTransaction mFragmentTransaction,@NonNull AnimType animType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (isDestroyed()) {
                return;
            }
        }
        if (isFinishing()) {
            return;
        }
        Log.i(TAG,"load:" + tagfragment);
        if (!fragmentChangeing) {
            fragmentChangeing = true;
            if (tagfragment!=null) {
                if (mFragmentTransaction == null) {
                    mFragmentTransaction = getSupportFragmentManager().beginTransaction();
                }

                //動畫
                if (animType==AnimType.ZoomShow) {
                    mFragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                } else if (animType== AnimType.LeftInRightOut) {
                    mFragmentTransaction.setCustomAnimations(R.anim.push_left_in, R.anim.push_left_out,R.anim.back_left_in,R.anim.back_right_out);
                } else if (animType== AnimType.BottomInTopOut) {
                    mFragmentTransaction.setCustomAnimations(R.anim.push_bottom_in,R.anim.push_top_out,R.anim.push_bottom_in,R.anim.push_top_out);
                } else {
                    //NONE
                }

                List<Fragment> fragmentlist = getSupportFragmentManager().getFragments();
                if (fragmentlist != null)
                    for (int i = 0; i < fragmentlist.size(); i++) {
                        Fragment fragment = fragmentlist.get(i);
                        if (fragment != null) {
                            mFragmentTransaction.hide(fragment);
                        }
                    }

                fragmentIndex++;
                FragmentTagVo tagVo = new FragmentTagVo(fragmentIndex,animType==null?AnimType.NONE.toString():animType.toString());
                try {
                    mFragmentTransaction.add(containerId, tagfragment, JSONObject.toJSONString(tagVo));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mFragmentTransaction.show(tagfragment);
                mFragmentTransaction.commitAllowingStateLoss();
            }
            fragmentChangeing = false;
        }
    }

    public void pushMsgToSubFragments (boolean needShow,int pushCode,Parcelable data) {
        Intent intent = new Intent();
        intent.putExtra("data",data);
        pushMsgToSubFragments(needShow,pushCode,intent);
    }

    public void pushMsgToSubFragments (boolean needShow,int pushCode,Intent pushData) {
        pushMsgToSubFragments(getSupportFragmentManager(), needShow,pushCode, Activity.RESULT_OK, pushData);
    }


    public void pushMsgToSubFragments (@NonNull FragmentManager fragmentManager,boolean needShow,int requestCode,int resultCode, Intent data) {
        List<Fragment> fragmentlist =  fragmentManager.getFragments();
        if (fragmentlist!=null)
            for (Fragment fragment:fragmentlist) {
                if (fragment==null) {
                    continue;
                }
                if (fragment.isAdded()) {
                    if (needShow) {
                        if (!fragment.isHidden()) {
                            Log.i(TAG,"send to:" + fragment);
                            fragment.onActivityResult(requestCode, resultCode, data);
                        }
                    } else {
                        Log.i(TAG,"send to:" + fragment);
                        fragment.onActivityResult(requestCode, resultCode, data);
                    }

                    pushMsgToSubFragments(fragment.getChildFragmentManager(),needShow,requestCode,resultCode,data);
                }
            }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG,"onActivityResult:" + requestCode);
        try {
            super.onActivityResult(requestCode, resultCode, data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        pushMsgToSubFragments(getSupportFragmentManager(), false, requestCode, resultCode, data);
    }


    final List<Fragment> removeFragment = new ArrayList<>();
    public boolean removeFragment(Class clazz) {
        if (clazz == null) {
            return false;
        }
        Log.i(TAG,"try remove:" + clazz.getName());
        synchronized (removeFragment) {
            List<Fragment> fragmentlist = getSupportFragmentManager().getFragments();
            if (fragmentlist != null) {
                for (Fragment fragment : fragmentlist) {
                    if (fragment == null) {
                        continue;
                    }
                    if (clazz.getName().equals(fragment.getClass().getName())) {
                        Log.i(TAG,"remove:" + clazz.getName());
                        removeFragment.add(fragment);
                    }
                }

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                for (Fragment fragment : removeFragment) {
                    ft.remove(fragment);
                }
                ft.commitAllowingStateLoss();
                removeFragment.clear();
            }
            //                }
        }
        return true;
    }

    public void setCanBackPress(boolean can) {
        this.mCanBackPress = can;
    }

    protected boolean mCanBackPress = true;
    protected OnBackPressedListener onBackPressedListener;

    public void setOnBackPressedListener(OnBackPressedListener listener) {
        this.onBackPressedListener = listener;
    }

    @Override
    public void onBackPressed() {
        if (onBackPressedListener != null) {
            onBackPressedListener.OnBackKeyDown();
            return;
        }

        if (!mCanBackPress) {
            return;
        }
        backPressed();
    }


    /**
     * 返回fragment,动画版本的
     */
    public void backPressed () {
        boolean waitAnimEndUnLock = false;
        if (!fragmentChangeing) {
            fragmentChangeing = true;
            try {
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.executePendingTransactions();
                List<Fragment> fragmentlist = fragmentManager.getFragments();
                if (fragmentlist==null) {
                    finish();
                }
                Log.i(TAG,"list:" + fragmentlist);
                for (int i = fragmentlist.size()-1; i >= 0 ; i--) {
                    Fragment fragment = fragmentlist.get(i);
                    if (!(fragment == null || !fragment.isVisible())) {//可视且不是没有设置view的fragment
                        //应用自己的返回逻辑
                        //                        if (fragment instanceof ContainerFragment) {
                        //                            ContainerFragment containerFragment = (ContainerFragment)fragment;
                        //                            String fragmentName =containerFragment.content.getClass().getSimpleName();
                        //                            if (fragmentName.equals("home")) {
                        //                                finish();
                        //                                return;
                        //                            }
                        //                        }

                        Fragment backFragment = null;
                        Log.i(TAG,"tag:" + fragment.getTag());
                        FragmentTagVo tagVo = JSONObject.parseObject(fragment.getTag(),FragmentTagVo.class);
                        int fragIndex = tagVo.index-1;
                        Looper:
                        while (fragIndex >= 0) {
                            FindByTag:
                            for (AnimType animType:AnimType.values()) {
                                tagVo = new FragmentTagVo(fragIndex,animType.toString());
                                backFragment = fragmentManager.findFragmentByTag(JSONObject.toJSONString(tagVo));
                                if (backFragment!=null) {
                                    break FindByTag;
                                }
                            }
                            Log.i(TAG,"backFragment:" + backFragment);
                            if (backFragment != null) {
                                Log.i(TAG,"backFragment add?:" + backFragment.isAdded());
                                Log.i(TAG,"backFragment removing?:" + backFragment.isRemoving());
                            }
                            if (backFragment == null || !backFragment.isAdded() || backFragment.isRemoving()) {
                                --fragIndex;
                            } else {
                                break Looper;
                            }
                        }
                        Log.i(TAG,"currFragment:" + fragment.getClass().getSimpleName());
                        Log.i(TAG,"backFragment:" + backFragment);

                        if (backFragment != null) {
                            tagVo = JSONObject.parseObject(fragment.getTag(),FragmentTagVo.class);
                            AnimType exitAnim = null;
                            if(tagVo!=null)
                                for (AnimType animType:AnimType.values()) {
                                    if (animType.toString().equals(tagVo.animType.toString())) {
                                        exitAnim = animType;
                                    }
                                }

                            final Fragment animShowFragment = backFragment;
                            final Fragment animHideFragment = fragment;
                            if (exitAnim==null || exitAnim==AnimType.NONE) {
                                FragmentTransaction mFragmentTransaction = getSupportFragmentManager().beginTransaction();
                                mFragmentTransaction.show(animShowFragment);
                                mFragmentTransaction.remove(animHideFragment);
                                mFragmentTransaction.commitAllowingStateLoss();
                            } else if (exitAnim==AnimType.ZoomShow) {
                                FragmentTransaction mFragmentTransaction = getSupportFragmentManager().beginTransaction();
                                mFragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                                mFragmentTransaction.show(animShowFragment);
                                mFragmentTransaction.remove(animHideFragment);
                                mFragmentTransaction.commitAllowingStateLoss();
                            } else {
                                FragmentTransaction mFragmentTransaction = getSupportFragmentManager().beginTransaction();
                                int[] animRes = AnimType.getAnimRes(exitAnim);
                                mFragmentTransaction.setCustomAnimations(animRes[2],animRes[3]);
                                mFragmentTransaction.show(animShowFragment);
                                mFragmentTransaction.remove(animHideFragment);
                                mFragmentTransaction.commitAllowingStateLoss();
                            }
                        } else {
                            finish();
                        }
                    }//end if (!(fragment == null || !fragment.isVisible())) {//可视的
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //KeyboradUtil.hideSoftKeyboard(this);
            if (!waitAnimEndUnLock) {
                fragmentChangeing = false;
            }
        }
    }

}
