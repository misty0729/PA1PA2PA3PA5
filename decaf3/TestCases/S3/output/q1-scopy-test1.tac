VTABLE(_A) {
    <empty>
    A
    _A.seta;
    _A.getA;
}

VTABLE(_Father) {
    <empty>
    Father
    _Father.setfield;
    _Father.seta;
    _Father.getA;
}

VTABLE(_Main) {
    <empty>
    Main
}

FUNCTION(_A_New) {
memo ''
_A_New:
    _T8 = 8
    parm _T8
    _T9 =  call _Alloc
    _T10 = 0
    *(_T9 + 4) = _T10
    _T11 = VTBL <_A>
    *(_T9 + 0) = _T11
    return _T9
}

FUNCTION(_Father_New) {
memo ''
_Father_New:
    _T12 = 12
    parm _T12
    _T13 =  call _Alloc
    _T14 = 0
    *(_T13 + 4) = _T14
    *(_T13 + 8) = _T14
    _T15 = VTBL <_Father>
    *(_T13 + 0) = _T15
    return _T13
}

FUNCTION(_Main_New) {
memo ''
_Main_New:
    _T16 = 4
    parm _T16
    _T17 =  call _Alloc
    _T18 = VTBL <_Main>
    *(_T17 + 0) = _T18
    return _T17
}

FUNCTION(_A.seta) {
memo '_T0:4 _T1:8'
_A.seta:
    _T19 = *(_T0 + 4)
    *(_T0 + 4) = _T1
}

FUNCTION(_A.getA) {
memo '_T2:4'
_A.getA:
    _T20 = *(_T2 + 4)
    return _T20
}

FUNCTION(_Father.setfield) {
memo '_T3:4 _T4:8'
_Father.setfield:
    _T21 = *(_T3 + 4)
    *(_T3 + 4) = _T4
    _T22 = *(_T3 + 8)
    _T23 =  call _A_New
    *(_T3 + 8) = _T23
}

FUNCTION(_Father.seta) {
memo '_T5:4 _T6:8'
_Father.seta:
    _T24 = *(_T5 + 8)
    parm _T24
    parm _T6
    _T25 = *(_T24 + 0)
    _T26 = *(_T25 + 8)
    call _T26
}

FUNCTION(_Father.getA) {
memo '_T7:4'
_Father.getA:
    _T27 = *(_T7 + 8)
    parm _T27
    _T28 = *(_T27 + 0)
    _T29 = *(_T28 + 12)
    _T30 =  call _T29
}

FUNCTION(main) {
memo ''
main:
    _T33 =  call _Father_New
    _T32 = _T33
    _T34 = 5
    parm _T32
    parm _T34
    _T35 = *(_T32 + 0)
    _T36 = *(_T35 + 8)
    call _T36
    _T37 = 10
    parm _T32
    parm _T37
    _T38 = *(_T32 + 0)
    _T39 = *(_T38 + 12)
    call _T39
    _T40 =  call _Father_New
    _T31 = _T40
    _T41 = 99
    parm _T31
    parm _T41
    _T42 = *(_T31 + 0)
    _T43 = *(_T42 + 8)
    call _T43
    _T44 = 88
    parm _T31
    parm _T44
    _T45 = *(_T31 + 0)
    _T46 = *(_T45 + 12)
    call _T46
    _T47 = 12
    parm _T47
    _T48 =  call _Alloc
    _T49 = *(_T32 + 4)
    *(_T48 + 4) = _T49
    _T50 = *(_T32 + 8)
    *(_T48 + 8) = _T50
    _T51 = VTBL <_Father>
    *(_T48 + 0) = _T51
    parm _T32
    _T52 = *(_T32 + 0)
    _T53 = *(_T52 + 16)
    _T54 =  call _T53
    parm _T54
    call _PrintInt
    parm _T48
    _T55 = *(_T48 + 0)
    _T56 = *(_T55 + 16)
    _T57 =  call _T56
    parm _T57
    call _PrintInt
    _T58 = 5
    parm _T32
    parm _T58
    _T59 = *(_T32 + 0)
    _T60 = *(_T59 + 12)
    call _T60
    parm _T32
    _T61 = *(_T32 + 0)
    _T62 = *(_T61 + 16)
    _T63 =  call _T62
    parm _T63
    call _PrintInt
    parm _T48
    _T64 = *(_T48 + 0)
    _T65 = *(_T64 + 16)
    _T66 =  call _T65
    parm _T66
    call _PrintInt
}

