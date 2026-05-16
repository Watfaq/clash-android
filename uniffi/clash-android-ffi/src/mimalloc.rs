#[cfg(feature = "mimalloc")]
unsafe extern "C" {
    /// https://microsoft.github.io/mimalloc/group__extended.html#ga1ea89844e2494f81b30d49b4cec80fb2
    fn mi_process_info(
        elapsed_msecs: *mut usize,
        user_msecs: *mut usize,
        system_msecs: *mut usize,
        current_rss: *mut usize,
        peak_rss: *mut usize,
        current_commit: *mut usize,
        peak_commit: *mut usize,
        page_faults: *mut usize,
    );
}

#[cfg(feature = "mimalloc")]
#[derive(uniffi::Record)]
pub struct MimallocStats {
    pub elapsed_msecs: u64,
    pub user_msecs: u64,
    pub system_msecs: u64,
    /// Current resident set size (bytes)
    pub current_rss: u64,
    /// Peak resident set size (bytes)
    pub peak_rss: u64,
    /// Current committed memory (bytes)
    pub current_commit: u64,
    /// Peak committed memory (bytes)
    pub peak_commit: u64,
    pub page_faults: u64,
}

#[cfg(feature = "mimalloc")]
#[uniffi::export]
pub fn get_mimalloc_stats() -> MimallocStats {
    let mut elapsed_msecs: usize = 0;
    let mut user_msecs: usize = 0;
    let mut system_msecs: usize = 0;
    let mut current_rss: usize = 0;
    let mut peak_rss: usize = 0;
    let mut current_commit: usize = 0;
    let mut peak_commit: usize = 0;
    let mut page_faults: usize = 0;
    unsafe {
        mi_process_info(
            &raw mut elapsed_msecs,
            &raw mut user_msecs,
            &raw mut system_msecs,
            &raw mut current_rss,
            &raw mut peak_rss,
            &raw mut current_commit,
            &raw mut peak_commit,
            &raw mut page_faults,
        );
    }
    MimallocStats {
        elapsed_msecs: elapsed_msecs as u64,
        user_msecs: user_msecs as u64,
        system_msecs: system_msecs as u64,
        current_rss: current_rss as u64,
        peak_rss: peak_rss as u64,
        current_commit: current_commit as u64,
        peak_commit: peak_commit as u64,
        page_faults: page_faults as u64,
    }
}
