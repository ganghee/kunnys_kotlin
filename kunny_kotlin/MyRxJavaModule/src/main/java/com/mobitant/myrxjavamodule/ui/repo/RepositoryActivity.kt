package com.mobitant.myrxjavamodule.ui.repo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.api.provideGithubApi
import com.androidhuman.example.simplegithub.ui.GlideApp
import com.mobitant.myrxjavamodule.R
import com.mobitant.myrxjavamodule.api.model.GithubRepo
import com.mobitant.myrxjavamodule.api.provideGithubApi
import com.mobitant.myrxjavamodule.extensions.plusAssign
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_repository.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class RepositoryActivity : AppCompatActivity() {

    companion object {

        const val KEY_USER_LOGIN = "user_login"

        const val KEY_REPO_NAME = "repo_name"
    }

    internal val api by lazy { provideGithubApi(this) }

    //disposables 객체생성
    //internal var repoCall: Call<GithubRepo>? = null
    internal var disposable : CompositeDisposable()

    internal val dateFormatInResponse = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())

    internal val dateFormatToShow = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository)

        val login = intent.getStringExtra(KEY_USER_LOGIN) ?: throw IllegalArgumentException(
                "No login info exists in extras")
        val repo = intent.getStringExtra(KEY_REPO_NAME) ?: throw IllegalArgumentException(
                "No repo info exists in extras")

        showRepositoryInfo(login, repo)
    }

    override fun onStop() {
        super.onStop()
        disposable.clear()
    }

    //Observable 객체를 만들어서 실행하기
    private fun showRepositoryInfo(login: String, repoName: String) {

        disposable += api.getRepository(login, repoName)

                //이 이후에 실행되는 코드는 모두 메인 스레드에서 실행합니다.
                .observeOn(AndroidSchedulers.mainThread())

                //구독할 때 수행할 작업을 구현합니다.
                .doOnSubscribe { showProgress() }

                //에러가 발생했을 때 수행할 작업을 구현합니다.
                .doOnError { hideProgress(true) }

                //옵저버블을 구독합니다.
                .subscribe({ repo ->
                    GlideApp.with(this@RepositoryActivity)
                            .load(repo.owner.avatarUrl)
                            .into(ivActivityRepositoryProfile)

                    tvActivityRepositoryName.text = repo.fullName
                    tvActivityRepositoryStars.text = resources
                            .getQuantityString(R.plurals.star, repo.stars, repo.stars)
                    if (null == repo.description) {
                        tvActivityRepositoryDescription.setText(R.string.no_description_provided)
                    } else {
                        tvActivityRepositoryDescription.text = repo.description
                    }
                    if (null == repo.language) {
                        tvActivityRepositoryLanguage.setText(R.string.no_language_specified)
                    } else {
                        tvActivityRepositoryLanguage.text = repo.language
                    }

                    try {
                        val lastUpdate = dateFormatInResponse.parse(repo.updatedAt)
                        tvActivityRepositoryLastUpdate.text = dateFormatToShow.format(lastUpdate)
                    } catch (e: ParseException) {
                        tvActivityRepositoryLastUpdate.text = getString(R.string.unknown)
                    }
                }){
                    //에러블록
                    //네트워크 오류나 데이터 처리 오류 등
                    //작업이 정상적으로 완료되지 않았을 때 호출
                    showError(it.message)
                }
    }

    private fun showProgress() {
        llActivityRepositoryContent.visibility = View.GONE
        pbActivityRepository.visibility = View.VISIBLE
    }

    private fun hideProgress(isSucceed: Boolean) {
        llActivityRepositoryContent.visibility = if (isSucceed) View.VISIBLE else View.GONE
        pbActivityRepository.visibility = View.GONE
    }

    private fun showError(message: String?) {
        with(tvActivityRepositoryMessage) {
            text = message ?: "Unexpected error."
            visibility = View.VISIBLE
        }
    }
}
