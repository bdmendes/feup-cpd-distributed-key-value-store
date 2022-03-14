#![allow(overflowing_literals)]

fn main() {
    println!("Hello, world!");

    unsafe {
        let mut values: [i64; 1] = [0];
        let mut test: [i64; 1000000] = [1; 1000000];
        let retval = papi_sys::PAPI_library_init(papi_sys::PAPI_VER_CURRENT);
        if retval != papi_sys::PAPI_VER_CURRENT {
            println!("no");
            return;
        }
        let mut event_set = 0;

        papi_sys::PAPI_create_eventset(&mut event_set);
        papi_sys::PAPI_add_event(event_set, 2 | 0x80000000);

        papi_sys::PAPI_start(event_set);
        
        for n in 1..1000000 {
            test[n] = 1 * 3 + 2 / 5;
        }

        papi_sys::PAPI_stop(event_set, (&mut values) as *mut i64);
        println!("L1 DCM: {} \n", values[0]);
    }
}
