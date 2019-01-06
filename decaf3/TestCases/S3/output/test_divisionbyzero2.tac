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
    _T4 = "testing division by 0 runtime error2\n"
    parm _T4
    call _PrintString
    _T5 = 13
    _T6 = 0
    _T7 = "Decaf runtime error: Division by zero error.\n"
    _T8 = 0
    _T9 = (_T6 == _T8)
    if (_T9 == 0) branch _L10
    parm _T7
    call _PrintString
    call _Halt
_L10:
    _T10 = (_T5 % _T6)
    _T3 = _T10
    _T11 = "end"
    parm _T11
    call _PrintString
}

