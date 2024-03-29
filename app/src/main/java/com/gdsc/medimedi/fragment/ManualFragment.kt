package com.gdsc.medimedi.fragment

import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.SUCCESS
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.gdsc.medimedi.R
import com.gdsc.medimedi.databinding.FragmentManualBinding
import com.gdsc.medimedi.databinding.FragmentSettingsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import java.util.*

class ManualFragment : Fragment(), TextToSpeech.OnInitListener {
    private var _binding: FragmentManualBinding? = null
    private val binding get() = _binding!!
    private lateinit var navController : NavController
    private lateinit var tts: TextToSpeech

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualBinding.inflate(inflater, container, false)
        return binding.root
    }

    // onCreateView()의 리턴값이 onViewCreated()의 매개변수로 전달됨.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        tts = TextToSpeech(this.context, this)

        // 한번 클릭하면 텍스트 모드, 더블 클릭하면 음성 모드
        binding.frameLayout.setOnClickListener(object : DoubleClickListener() {
            override fun onSingleClick() {
                decideFirstScreen("텍스트 모드")
            }
            override fun onDoubleClick() {
                decideFirstScreen("음성 모드")
            }
        })

        // 길게 누르면 안내음 다시 재생
        binding.frameLayout.setOnLongClickListener {
            speakOut(getString(R.string.app_manual))
            return@setOnLongClickListener true
        }
    }

    // Case1: 매뉴얼 화면에서 모드 선택 -> 첫 로그인 (tts) -> 홈 (null)
    // Case2: 매뉴얼 화면에서 모드 선택 -> 자동 로그인 -> 홈 (tts)
    // todo: 선택한 모드에 따라 설정 화면의 스위치 on/off 해줘야 함.
    private fun decideFirstScreen(mode: String) {
        val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
        if(account != null){ // 자동 로그인 후, 곧바로 홈 화면 진입
            val action = ManualFragmentDirections.actionManualFragmentToHomeFragment(mode)
            findNavController().navigate(action)
        }else{ // 로그인 화면 진입
            val action = ManualFragmentDirections.actionManualFragmentToLoginFragment(mode)
            findNavController().navigate(action)
        }
    }

    override fun onStart() { // 자동 로그인
        super.onStart()
        // Check for existing Google Sign In account,
        // if the user is already signed in, the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
        updateUI(account)
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if (account == null) { // 로그인 실패 (로그인 버튼 표시)
            Log.e("SignIn", "Fail")
        } else { // 로그인 성공 (서버에 유저 정보 보낸 뒤 홈화면으로 넘어가기)
            Log.e("SignIn", "Success")
        }
    }

    override fun onInit(status: Int) {
        // TTS 객체가 정상적으로 초기화 되면
        if (status == SUCCESS) {
            tts.language = Locale.KOREA // 언어 설정
            tts.setPitch(0.6F) // 음성 톤 높이 지정
            tts.setSpeechRate(1.0F) // 음성 속도 지정
            speakOut(getString(R.string.app_manual))
        } else { // 초기화 실패
            Log.e("TTS", "Initialization Failed!")
        }
    }

    private fun speakOut(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    abstract class DoubleClickListener : View.OnClickListener {
        abstract fun onDoubleClick()
        abstract fun onSingleClick()

        companion object {
            private const val DEFAULT_QUALIFICATION_SPAN: Long = 300
        }

        private var isSingleEvent = false
        private val doubleClickSpanInMillis: Long = DEFAULT_QUALIFICATION_SPAN
        private var timestampLastClick: Long
        private val handler: Handler
        private val runnable: Runnable

        init { // 생성자
            timestampLastClick = 0
            handler = Handler()
            runnable = Runnable {
                if (isSingleEvent) {
                    onSingleClick()
                }
            }
        }

        override fun onClick(v: View) {
            // 더블 클릭
            if (SystemClock.elapsedRealtime() - timestampLastClick < doubleClickSpanInMillis) {
                isSingleEvent = false
                handler.removeCallbacks(runnable)
                onDoubleClick()
                return
            }else{ // 싱글 클릭
                isSingleEvent = true
                handler.postDelayed(runnable, DEFAULT_QUALIFICATION_SPAN)
                timestampLastClick = SystemClock.elapsedRealtime()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts.stop()
        tts.shutdown()
        _binding = null
    }
}
