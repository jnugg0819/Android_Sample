package com.example.coronastagram.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.coronastagram.LoginActivity
import com.example.coronastagram.MainActivity
import com.example.coronastagram.R
import com.example.coronastagram.navigation.model.ContentDTO
import com.example.coronastagram.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment(){
    var fragmentView :View?=null
    var firestore :FirebaseFirestore?=null
    var uid : String?=null
    var auth: FirebaseAuth?=null
    var currentUserUid: String?=null
    companion object {
        var PICK_PROFILE_FROM_ALBUM=10
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView=LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)
        uid=arguments?.getString("destinationUid")
        firestore= FirebaseFirestore.getInstance()
        auth=FirebaseAuth.getInstance()
        currentUserUid=auth?.currentUser?.uid

        if(uid==currentUserUid){
            //My page
            fragmentView?.account_btn_follow_signout?.text=getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow)
            var mainavtivity=(activity as MainActivity)
            mainavtivity?.toolbar_username?.text=arguments?.getString("userId")
            mainavtivity?.toolbar_btn_back?.setOnClickListener {
                mainavtivity.bottom_navigation.selectedItemId=R.id.action_home
            }
            mainavtivity?.toolbar_title_image?.visibility=View.GONE
            mainavtivity?.toolbar_username?.visibility=View.VISIBLE
            mainavtivity?.toolbar_btn_back?.visibility=View.VISIBLE
            fragmentView?.account_btn_follow_signout?.setOnClickListener{
                requestFollow()
            }

        }

        fragmentView?.account_recyclerview?.adapter=UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager=GridLayoutManager(activity!!,3)

        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent=Intent(Intent.ACTION_PICK)
            photoPickerIntent.type="image/*"
            activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)

        }
        getProfileImage()
       // getFollowerAndFollowing()

        return fragmentView
    }
    /*
    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener{documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot==null)return@addSnapshotListener
            var followDTO=documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount!=null)
            {
                fragmentView?.account_tv_following_count?.text=followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount!=null)
            {
                fragmentView?.account_tv_follower_count?.text=followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(currentUserUid!!)){
                    fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(activity!!,R.color.colorLightGray),PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid!=currentUserUid){
                        fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow)
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter=null
                    }
                }
            }


        }
    }
'*/
    fun requestFollow(){
        //Save data to my account
        var tsDocFollowing=firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO =transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO==null){
                followDTO= FollowDTO()
                followDTO!!.followerCount=1
                followDTO!!.followers[uid!!]=true

                transaction.set(tsDocFollowing,followDTO)
                return@runTransaction
            }
            if(followDTO.followings.containsKey(uid)){
                //It remove following third person when a third person follow me
                followDTO?.followingCount=followDTO?.followingCount-1
                followDTO?.followers?.remove(uid)
            }else
            {
                //It remove following third person when a third person do not follow me
                followDTO?.followingCount=followDTO?.followingCount+1
                followDTO?.followers[uid!!]=true

            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }
        //Save data to third person
        var tsDocFollower=firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO=transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO==null){
                followDTO=FollowDTO()
                followDTO!!.followerCount=1
                followDTO!!.followers[currentUserUid!!]=true

                transaction.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }
            if(followDTO!!.followers.containsKey(currentUserUid)){
                //It cancel my follower when I follow a third person
                followDTO!!.followerCount=followDTO!!.followerCount-1
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                //It add my follower when i don't follow a third person
                followDTO!!.followerCount=followDTO!!.followerCount+1
                followDTO!!.followers[currentUserUid!!]=true
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }
    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener{ documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot==null) return@addSnapshotListener
            if(documentSnapshot.data!=null){
                var url=documentSnapshot?.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)

            }

        }
    }
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init{
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener{querySnapshot, firebaseFirestoreException ->
                //Sometimes, This code return null of querySnapshot when it signout
                if(querySnapshot==null) return@addSnapshotListener

                //Get data
                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView?.account_tv_post_count?.text=contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var width=resources.displayMetrics.widthPixels/3
            var imageView= ImageView(p0.context)
            imageView.layoutParams=LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var imageView=(p0 as CustomViewHolder).imageView
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)
        }

    }

}