package com.mobitant.myrxjavamodule.extensions

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

//CompositeDisposable에서 add함수를 호출 하는 대신에 +=연산자를 사용하여
//CompositeDisposable 객체에 디스포저블 객체를 추가하도록 한다면
//더 직관적이면서도 편리하게 코드를 작성할 수 있습니다.
//이를 위해 CompositeDisposable 클래스의 += 연산자를 오버로딩합니다.

//CompositeDisposable의 '+='연산자 뒤에 Disposable 타입이 오는 경우를 재정의 합니다.
operator fun CompositeDisposable.plusAssign(disposable: Disposable){

    // CompositeDisposable.add()함수를 호출 합니다.
    this.add(disposable)
}