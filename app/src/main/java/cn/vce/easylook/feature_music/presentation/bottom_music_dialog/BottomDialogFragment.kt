package cn.vce.easylook.feature_music.presentation.bottom_music_dialog

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.vce.easylook.R
import cn.vce.easylook.base.BaseBottomSheetDialogFragment
import cn.vce.easylook.databinding.DialogLayoutBinding
import cn.vce.easylook.databinding.ItemDialogBinding
import cn.vce.easylook.feature_music.models.MusicInfo
import cn.vce.easylook.feature_music.models.PlaylistType
import cn.vce.easylook.feature_music.service.DownloadService
import cn.vce.easylook.utils.ConvertUtils
import com.drake.brv.utils.linear
import com.drake.brv.utils.setup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 歌曲操作类
 *
 */
@AndroidEntryPoint
class BottomDialogFragment : BaseBottomSheetDialogFragment<DialogLayoutBinding>() {


    private lateinit var viewModel: BottomDialogVM
    private var itemData = mutableMapOf(
        R.string.popup_play_next to R.drawable.ic_queue_play_next,
        R.string.popup_download to R.drawable.ic_queue_play_next,
        R.string.popup_add_to_collection to R.drawable.ic_add_love,
        R.string.popup_add_to_playlist to R.drawable.ic_playlist_add,
        R.string.popup_album to R.drawable.ic_album,
        R.string.popup_artist to R.drawable.ic_art_track,
        R.string.popup_delete to R.drawable.ic_delete
    )

    private lateinit var musicInfo: MusicInfo

    private val data = mutableListOf<PopupItemBean>()


    private lateinit var downloadBinder: DownloadService.DownloadBinder

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            downloadBinder = service as DownloadService.DownloadBinder
            downloadBinder.startDownload(musicInfo)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Log.d("MyService", "onServiceDisconnected")
        }
    }


    override fun getLayoutId(): Int? = R.layout.dialog_layout

    override fun initActivityViewModel() {
        mainVM = getActivityViewModel()
    }

    override fun initFragmentViewModel() {
        viewModel = getFragmentViewModel()
    }

    override fun init(savedInstanceState: Bundle?) {
        initView()
    }

    override fun observe() {
        viewModel.eventFlow.onEach { event ->
            when(event){
                is BottomDialogVM.UiEvent.SaveMusic -> {
                    lifecycleScope.launch {
                        val rootView = mActivity.findViewById<View>(R.id.drawer_layout)
                        val snackbar = Snackbar.make(rootView, "${musicInfo.name}" + getString(R.string.popup_add_to_collection), Snackbar.LENGTH_SHORT)
                            .setAction("Undo") {
                                viewModel.onEvent(BottomDialogEvent.RestoreMusicToPlaylist(musicInfo))
                                Toast.makeText(mActivity, "撤销${musicInfo.name}" + getString(R.string.popup_add_to_collection),
                                    Toast.LENGTH_SHORT).show()
                            }
                        val mView: View = snackbar.view
                        mView.setBackgroundColor(mActivity.getColor(R.color.white))
                        snackbar.show()
                        hide()
                    }
                }
                is BottomDialogVM.UiEvent.ShwoSnackbar -> {
                    Toast.makeText(mActivity, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }.launchIn(lifecycleScope)
    }

    private var mBehavior: BottomSheetBehavior<*>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mBehavior = BottomSheetBehavior.from(binding.root?.parent as View)
    }


    override fun initView() {
        this.isHidden
        itemData.forEach(){
            if (it.key == R.string.popup_add_to_collection){
                data.add(PopupItemBean(getString(it.key) , it.value, it.key))
            }else if (it.key == R.string.popup_album){
                data.add(PopupItemBean(getString(it.key, musicInfo.album?.name), it.value, it.key))
            }else if (it.key == R.string.popup_artist){
                data.add(PopupItemBean(getString(it.key, ConvertUtils.getArtist(musicInfo?.artists)) , it.value, it.key))
            }else if (it.key == R.string.popup_delete){
                if (musicInfo.pid.isNotEmpty()) {
                    data.add(PopupItemBean(getString(it.key, ConvertUtils.getArtist(musicInfo?.artists)) , it.value, it.key))
                }
            }else{
                data.add(PopupItemBean(getString(it.key), it.value, it.key))
            }
        }
        binding.apply{
            titleTv?.text = musicInfo.name//music?.title
            subTitleTv?.text = ConvertUtils.getArtistAndAlbum(musicInfo?.artists,
                musicInfo?.album?.name
            )
            bottomSheetRv.linear().setup {
                addType<PopupItemBean>(R.layout.item_dialog)
                onBind {
                    val model = getModel<PopupItemBean>()
                    val binding = getBinding<ItemDialogBinding>()
                    binding.apply {
                        tvTitle.text = model.title
                        ivIcon.setImageResource(model.icon)
                        //mBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }
                R.id.item.onFastClick {
                    val model = getModel<PopupItemBean>()
                    when(model.key){
                        R.string.popup_play_next-> { // 下一首

                        }
                        R.string.popup_download -> {
                            //mainVM.onEvent(MainEvent.DownloadMusic(arrayListOf(musicInfo)))
                            val intent = Intent(mActivity, DownloadService::class.java)
                            intent.action = "android.intent.action.DownloadMusic"
                            mActivity.bindService(intent, connection,  Context.BIND_AUTO_CREATE)
                            //mActivity.unbindService(connection)
                        }
                        R.string.popup_add_to_collection -> { //收藏
                            musicInfo.apply {
                                pid = PlaylistType.LOVE.toString()
                                viewModel.onEvent(BottomDialogEvent.SaveMusicToPlaylist(musicInfo))
                            }
                        }
                    }
                    this@BottomDialogFragment.hide()
                }
            }.models = data
        }
    }

    fun show(context: Context, musicInfo: MusicInfo) {
        this.musicInfo = musicInfo
        mActivity = context as AppCompatActivity
        mContext = context
        val ft = mActivity.supportFragmentManager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }

}

data class PopupItemBean(val title: String = "", val icon: Int = 0, val key:Int)