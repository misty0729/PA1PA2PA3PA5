VTABLE(_Main) {
    <empty>
    Main
}

FUNCTION(_Main_New) {
memo ''
_Main_New:
    _T0 = 4
    parm _T0
    _T1 =  call _Alloc
    _T2 = VTBL <_Main>
    *(_T1 + 0) = _T2
    return _T1
}

FUNCTION(main) {
memo ''
main:
    _T4 = "pa1"
    _T5 = 2
    _T6 = 0
    _T7 = (_T5 < _T6)
    if (_T7 == 0) branch _L10
    _T8 = "Decaf runtime error: The length of the created array should not be less than 0.\n"
    parm _T8
    call _PrintString
    call _Halt
_L10:
    _T9 = 4
    _T10 = (_T9 * _T5)
    _T11 = (_T9 + _T10)
    parm _T11
    _T12 =  call _Alloc
    *(_T12 + 0) = _T5
    _T13 = 4
    _T13 = _T4
    _T12 = (_T12 + _T11)
_L11:
    _T11 = (_T11 - _T9)
    if (_T11 == 0) branch _L12
    _T12 = (_T12 - _T9)
    *(_T12 + 0) = _T4
    branch _L11
_L12:
    _T3 = _T12
    _T15 = 0
    _T17 = *(_T3 - 4)
    _T18 = (_T15 < _T17)
    if (_T18 == 0) branch _L13
    _T19 = 0
    _T20 = (_T15 < _T19)
    if (_T20 != 0) branch _L13
    _T21 = 4
    _T22 = (_T15 * _T21)
    _T23 = (_T3 + _T22)
    _T25 = *(_T23 + 0)
    _T16 = _T25
    branch _L14
_L13:
    _T26 = "pa2"
    _T16 = _T26
    branch _L14
_L14:
    _T14 = _T16
    _T28 = 2
    _T30 = *(_T3 - 4)
    _T31 = (_T28 < _T30)
    if (_T31 == 0) branch _L15
    _T32 = 0
    _T33 = (_T28 < _T32)
    if (_T33 != 0) branch _L15
    _T34 = 4
    _T35 = (_T28 * _T34)
    _T36 = (_T3 + _T35)
    _T38 = *(_T36 + 0)
    _T29 = _T38
    branch _L16
_L15:
    _T39 = "pa3"
    _T29 = _T39
    branch _L16
_L16:
    _T27 = _T29
    parm _T14
    call _PrintString
    parm _T27
    call _PrintString
}

