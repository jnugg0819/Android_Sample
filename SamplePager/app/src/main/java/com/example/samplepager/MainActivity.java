package com.example.samplepager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ViewPager viewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button=findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                viewPager.setCurrentItem(1);
            }
        });

        viewPager=findViewById(R.id.pager);
        viewPager.setOffscreenPageLimit(3);

        MyPagerAdapter adapter=new MyPagerAdapter(getSupportFragmentManager());

        Fragment1 fragment1=new Fragment1();
        adapter.addItem(fragment1);

        Fragment2 fragment2=new Fragment2();
        adapter.addItem(fragment2);

        Fragment3 fragment3=new Fragment3();
        adapter.addItem(fragment3);

        viewPager.setAdapter(adapter);
    }

    class MyPagerAdapter extends FragmentStatePagerAdapter{
        ArrayList<Fragment> items=new ArrayList<Fragment>();
        public MyPagerAdapter(FragmentManager fm){
            super(fm);
        }

        public void addItem(Fragment item){
            items.add(item);
        }

        public Fragment getItem(int position){
            return items.get(position);
        }

        public int getCount(){
            return items.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return "페이지"+position;
        }


    }
}
