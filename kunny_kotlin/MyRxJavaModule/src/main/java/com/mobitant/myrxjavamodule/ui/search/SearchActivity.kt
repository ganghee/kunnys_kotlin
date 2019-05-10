package com.mobitant.myrxjavamodule.ui.search

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.jakewharton.rxbinding2.widget.RxSearchView
import com.jakewharton.rxbinding2.widget.queryTextChangeEvents
import com.mobitant.myrxjavamodule.R
import com.mobitant.myrxjavamodule.api.GithubApi
import com.mobitant.myrxjavamodule.api.model.GithubRepo
import com.mobitant.myrxjavamodule.api.provideGithubApi
import com.mobitant.myrxjavamodule.extensions.plusAssign
import com.mobitant.myrxjavamodule.ui.repo.RepositoryActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_search.*
import org.jetbrains.anko.startActivity
import java.lang.IllegalStateException

class SearchActivity : AppCompatActivity(), SearchAdapter.ItemClickListener {

    internal lateinit var menuSearch: MenuItem

    internal lateinit var searchView: SearchView

    internal val adapter by lazy {
        SearchAdapter().apply { setItemClickListener(this@SearchActivity) }
    }

    internal val api: GithubApi by lazy { provideGithubApi(this) }

    //여러 디스포저블 객체를 관리할 수 있는 CompositeDisposible 객체를 초기화 합니다.
    //internal var searchCall: Call<RepoSearchResponse>? = null
    internal val disposables = CompositeDisposable()

    //viewDisposables 프로퍼티를 추가합니다.
    internal val viewDisposables = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        with(rvActivitySearchList) {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_search, menu)
        menuSearch = menu.findItem(R.id.menu_activity_search_query)

        searchView = (menuSearch.actionView as SearchView)
        //searchView에서 발생하는 이벤트를 옵서버블 형태로 받습니다.
        //searchView 인스턴스에서 RxBinding에서 제공하는 함수를 직접 호출합니다.
        viewDisposables += searchView.queryTextChangeEvents()

                //검색을 수행했을 때 발생한 이벤트만 받습니다.
                .filter { it.isSubmitted }

                //이벤트에서 검색어 텍스트{CharSequence}를 추출합니다.
                .map { it.queryText() }

                //빈 문자열이 아닌 검색어만 받습니다.
                .filter { it.isNotEmpty() }

                //검색어를 String 형태로 변환합니다.
                .map { it.toString() }

                //이 이후에 수행되는 코드는 모두 메인 스레드에서 실행합니다.
                //RxAndroid에서 제공하는 스케줄러인 AndroidSchedulers.mainThread()를 사용합니다.
                .observeOn(AndroidSchedulers.mainThread())

                //옵서버블을 구독합니다.
                .subscribe { query ->
                    //검색 절차를 수행합니다.
                    updateTitle(query)
                    hideSoftKeyboard()
                    collapseSearchView()
                    searchRepository(query)
                }
        menuSearch.expandActionView()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (R.id.menu_activity_search_query == item.itemId) {
            item.expandActionView()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()

        //액티비티가 완전히 종료되고 있는 경우에만 관리하고 있는 디스포저블을 해제합니다.
        //화면이 꺼지거나 다른 액티비티를 호출하여 엑티비티가 화면에서 사라지는 경우에는
        //해제하지 않습니다.
        if(isFinishing){
            viewDisposables.clear()
        }
    }

    override fun onItemClick(repository: GithubRepo) {
        startActivity<RepositoryActivity>(
                RepositoryActivity.KEY_USER_LOGIN to repository.owner.login,
                RepositoryActivity.KEY_REPO_NAME to repository.name)
    }

    private fun searchRepository(query: String) {
        clearResults()
        hideError()
        showProgress()

        disposables += api.searchRepository(query)

                //Observable 형태로 결과를 바꿔주기 위해 flatMap을 사용합니다.
                .flatMap{
                    if( 0 == it.totalCount){
                        //검색 결과가 없을 경우
                        //에러를 발생시켜 에러 메시지를 표시하도록 합니다.
                        Observable.error(IllegalStateException("No search result"))
                    }else{
                        //검색 결과 리스트를 다음 스트림으로 전달 합니다.
                        Observable.just(it.items)
                    }
                }

                //구독할 때 수행할 작업을 구현합니다.
                .doOnSubscribe {
                    clearResults()
                    hideError()
                    showProgress()
                }

                //스트림이 종료될 때 수행할 작업을 구현합니다.
                .doOnTerminate { hideProgress() }

                //옵서버블을 구독합니다.
                .subscribe({ items ->
                    //API를 통해 검색 결과를 정상적으로 받았을 때 처리할 작업을 구현합니다.
                    //작업 중 오류가 발생하면 이 블록은 호출되지 않습니다.
                    with(adapter){
                        setItems(items)
                        notifyDataSetChanged()
                    }
                }){
                    //에러블록
                    //네트워크 오류나 데이터 처리 오류 등
                    //작업이 정상적으로 완료되지 않았을 때 호출됩니다.
                    showError(it.message)
                }
    }

    private fun updateTitle(query: String) {
        supportActionBar?.run { subtitle = query }
    }

    private fun hideSoftKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).run {
            hideSoftInputFromWindow(searchView.windowToken, 0)
        }
    }

    private fun collapseSearchView() {
        menuSearch.collapseActionView()
    }

    private fun clearResults() {
        with(adapter) {
            clearItems()
            notifyDataSetChanged()
        }
    }

    private fun showProgress() {
        pbActivitySearch.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        pbActivitySearch.visibility = View.GONE
    }

    private fun showError(message: String?) {
        with(tvActivitySearchMessage) {
            text = message ?: "Unexpected error."
            visibility = View.VISIBLE
        }
    }

    private fun hideError() {
        with(tvActivitySearchMessage) {
            text = ""
            visibility = View.GONE
        }
    }
}
