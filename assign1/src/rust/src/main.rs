#![allow(overflowing_literals)]
use std::time::Instant;

fn main() {
    // println!("Hello, world!");

    // unsafe {
    //     let mut values: [i64; 1] = [0];
    //     let mut test: [i64; 1000000] = [1; 1000000];
    //     let retval = papi_sys::PAPI_library_init(papi_sys::PAPI_VER_CURRENT);
    //     if retval != papi_sys::PAPI_VER_CURRENT {
    //         println!("no");
    //         return;
    //     }
    //     let mut event_set = 0;

    //     papi_sys::PAPI_create_eventset(&mut event_set);
    //     papi_sys::PAPI_add_event(event_set, 2 | 0x80000000);

    //     papi_sys::PAPI_start(event_set);
        
    //     for n in 1..1000000 {
    //         test[n] = 1 * 3 + 2 / 5;
    //     }

    //     papi_sys::PAPI_stop(event_set, (&mut values) as *mut i64);
    //     println!("L1 DCM: {} \n", values[0]);
    // }

    multi_line(1000);
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

