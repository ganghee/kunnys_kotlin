package com.mobitant.myrxjavamodule.ui.signin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.mobitant.myrxjavamodule.BuildConfig
import com.mobitant.myrxjavamodule.R
import com.mobitant.myrxjavamodule.api.model.GithubAccessToken
import com.mobitant.myrxjavamodule.api.provideAuthApi
import com.mobitant.myrxjavamodule.data.AuthTokenProvider
import com.mobitant.myrxjavamodule.extensions.plusAssign
import com.mobitant.myrxjavamodule.ui.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.internal.subscriptions.SubscriptionHelper.cancel
import kotlinx.android.synthetic.main.activity_sign_in.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.newTask
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignInActivity : AppCompatActivity() {

    internal val api by lazy { provideAuthApi() }

    internal val authTokenProvider by lazy { AuthTokenProvider(this) }

    //여러 디스포저블 객체를 조금 더 효율적으로 관리하기 위해,
    //CompositeDisposable을 사용 아래 코드 수정
    //internal var accessTokenCall: Call<GithubAccessToken>? = null
    internal val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        btnActivitySignInStart.setOnClickListener {
            val authUri = Uri.Builder().scheme("https").authority("github.com")
                    .appendPath("login")
                    .appendPath("oauth")
                    .appendPath("authorize")
                    .appendQueryParameter("client_id", BuildConfig.GITHUB_CLIENT_ID)
                    .build()

            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(this@SignInActivity, authUri)
        }

        if (null != authTokenProvider.token) {
            launchMainActivity()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        showProgress()

        val code = intent.data?.getQueryParameter("code")
                ?: throw IllegalStateException("No code exists")

        getAccessToken(code)
    }

    override fun onStop() {
        super.onStop()
        //disposable 해제
        disposables.clear()
    }
    //엑세스 토큰을 반환하는 옵서버블에 구독하면 디스포저블 객체가
    //생성 되는데, 이때 생성된 디스포저블 객체는 CompositeDisposable에서
    //관리 하도록 CompositeDisposable.add()함수를 사용하여 추가하고
    //받은 응답에서 map()함수를 사용하여 전달되는 데이터를 변경한다.
    private fun getAccessToken(code: String) {
        showProgress()

       disposables += api.getAccessToken(
                BuildConfig.GITHUB_CLIENT_ID, BuildConfig.GITHUB_CLIENT_SECRET, code)

               //REST API를 통해 받은 응답에서 엑세스 토큰만 추출한다.
               .map { it.accessToken }

               //이후에 수행되는 코드는 모두 메인 스레드에서 실행합니다.
               //RxAndroid에서 제공하는 스케줄러인 AndroidSchedulers.mainThread()를 사용합니다.
               .observeOn(AndroidSchedulers.mainThread())

               //구독할 때 수행할 작업을 구현합니다.
               .doOnSubscribe { showProgress() }

               //스트림이 종료될 때 수행할 작업을 구현합니다.
               .doOnTerminate{ hideProgress() }

               //옵서버블을 구독합니다.
               .subscribe({ token ->
                   //API를 통해 엑세스 토튼을 정상적으로 받았을 때 처리할 작업을 구현합니다.
                   //작업 중 오류가 나면 이 블록을 호출 되지 않습니다.
                   authTokenProvider.updateToken(token)
                   launchMainActivity()
               }){
                   //에러 블록
                   //네트워크 오류나 데이터 처리 오류 등
                   // 작업이 정상적으로 수행되지 않았을 때 호출
                   showError(it)
               }
    }

    private fun showProgress() {
        btnActivitySignInStart.visibility = View.GONE
        pbActivitySignIn.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        btnActivitySignInStart.visibility = View.VISIBLE
        pbActivitySignIn.visibility = View.GONE
    }

    private fun showError(throwable: Throwable) {
        longToast(throwable.message ?: "No message available")
    }

    private fun launchMainActivity() {
        startActivity(intentFor<MainActivity>().clearTask().newTask())
    }
}
