#![allow(overflowing_literals)]

/* PAPI constants */
const PAPI_OK: i32 = 0;
const PAPI_L1_DCM: i32 = -2147483648;
const PAPI_L2_DCM: i32 = -2147483646;

fn operation() {
    println!("Hello world!");
}

fn main() {
    /* Init PAPI */
    let mut event_set: i32 = papi_sys::PAPI_NULL;
    let mut values: [i64; 2] = [0, 0];
    unsafe {
        let r = papi_sys::PAPI_library_init(papi_sys::PAPI_VER_CURRENT);
        if r != papi_sys::PAPI_VER_CURRENT {
            println!("PAPI library version mismatch!");
            return;
        }
        papi_sys::PAPI_create_eventset(&mut event_set);
        papi_sys::PAPI_add_event(event_set, PAPI_L1_DCM);
        papi_sys::PAPI_add_event(event_set, PAPI_L2_DCM);
    }

    /* Operations */
    let mut l: bool = true;
    while l {
        unsafe {
            papi_sys::PAPI_start(event_set);
        }
        operation();
        l = false;
        unsafe {
            if papi_sys::PAPI_stop(event_set, (&mut values) as *mut i64) != PAPI_OK {
                println!("ERROR: Stop PAPI");
            }
            println!("L1 DCM: {}", values[0]);
            println!("L2 DCM: {}", values[1]);
            if papi_sys::PAPI_reset(event_set) != PAPI_OK {
                println!("FAIL reset");
            }
        }
    }

    /* Destroy PAPI */
    unsafe {
        if papi_sys::PAPI_remove_event(event_set, PAPI_L1_DCM) != PAPI_OK {
            println!("FAIL remove event");
        }
        if papi_sys::PAPI_remove_event(event_set, PAPI_L2_DCM) != PAPI_OK {
            println!("FAIL remove event");
        }
        if papi_sys::PAPI_destroy_eventset(&mut event_set) != PAPI_OK {
            println!("FAIL destroy");
        }
    }
}
