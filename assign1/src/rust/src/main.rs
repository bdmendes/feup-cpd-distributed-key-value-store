use std::time::Instant;

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
        multi_line(1000);
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

fn multi_line(m_size: usize) {
    let matrix_left : Vec<f64> = vec![1.0; m_size * m_size];
    let mut matrix_right : Vec<f64> = vec![0.0; m_size * m_size];
    let mut matrix_result : Vec<f64> = vec![0.0; m_size * m_size];

    for line in 0..m_size {
        for col in 0..m_size {
            matrix_right[line * m_size + col] = line as f64 + 1.0;
        }
    }

    let now = Instant::now();

    for line in 0..m_size {
        for iter in 0..m_size {
            for col in 0..m_size {
                matrix_result[line * m_size + col] += matrix_left[line * m_size + iter] * matrix_right[iter * m_size + col]
            }
        }
    }

    let elapsed = now.elapsed();
    println!("Time: {:3.3?}", elapsed);

    for col in 0..(std::cmp::min(10, m_size)) {
        print!("{} ", matrix_result[col])
    }
    println!("");
}

