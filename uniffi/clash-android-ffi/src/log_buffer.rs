use std::collections::VecDeque;
use std::sync::{Arc, Mutex, OnceLock};

use clash_lib::app::logging::LogEvent;

#[derive(Clone, uniffi::Record)]
pub struct LogEntry {
    pub timestamp: i64,
    pub level: String,
    pub message: String,
}

struct LogBuffer {
    inner: Arc<Mutex<VecDeque<LogEntry>>>,
    max_entries: usize,
}

impl LogBuffer {
    fn new(max_entries: usize) -> Self {
        Self {
            inner: Arc::new(Mutex::new(VecDeque::with_capacity(max_entries))),
            max_entries,
        }
    }

    fn push(&self, event: LogEvent) {
        let mut buf = self.inner.lock().unwrap();
        if buf.len() >= self.max_entries {
            buf.pop_front();
        }
        buf.push_back(LogEntry {
            timestamp: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap_or_default()
                .as_millis() as i64,
            level: format!("{:?}", event.level),
            message: event.msg,
        });
    }

    fn get_logs(&self) -> Vec<LogEntry> {
        self.inner.lock().unwrap().iter().cloned().collect()
    }

    fn clear(&self) {
        self.inner.lock().unwrap().clear();
    }
}

static LOG_BUFFER: OnceLock<Arc<LogBuffer>> = OnceLock::new();

pub fn ensure_log_buffer(max_entries: usize) {
    LOG_BUFFER.get_or_init(|| Arc::new(LogBuffer::new(max_entries)));
}

pub fn push_log(event: LogEvent) {
    if let Some(buf) = LOG_BUFFER.get() {
        buf.push(event);
    }
}

#[uniffi::export]
pub fn get_clash_logs() -> Vec<LogEntry> {
    LOG_BUFFER.get().map(|b| b.get_logs()).unwrap_or_default()
}

#[uniffi::export]
pub fn clear_clash_logs() {
    if let Some(buf) = LOG_BUFFER.get() {
        buf.clear();
    }
}
