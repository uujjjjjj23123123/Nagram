use std::f32::consts::PI;

/// Parameters defining a spring's tactile feel (SwiftUI / Apple style).
#[derive(Debug, Clone, Copy)]
pub struct SpringConfig {
    /// Response time in seconds (e.g. 0.25s for snappy, 0.4s for gentle)
    pub response: f32,
    /// Damping ratio: 1.0 = critically damped (no overshoot), < 1.0 = bouncy
    pub damping_ratio: f32,
}

impl Default for SpringConfig {
    fn default() -> Self {
        Self {
            response: 0.25,
            damping_ratio: 0.85,
        }
    }
}

#[derive(Debug, Clone, Copy, Default)]
pub struct SpringState {
    pub position: f32,
    pub velocity: f32,
    pub target: f32,
}

impl SpringState {
    pub fn new(initial: f32) -> Self {
        Self {
            position: initial,
            velocity: 0.0,
            target: initial,
        }
    }

    /// Advances the spring by dt seconds using symplectic Euler integration.
    pub fn step(&mut self, dt: f32, config: SpringConfig) {
        if dt <= 0.0 {
            return;
        }
        let clamped_dt = dt.min(0.05); // Prevent explosion on frame drop

        let omega = 2.0 * PI / config.response;
        let k = omega * omega; // Stiffness
        let c = 2.0 * config.damping_ratio * omega; // Damping

        let displacement = self.position - self.target;
        let force = -k * displacement - c * self.velocity;

        self.velocity += force * clamped_dt;
        self.position += self.velocity * clamped_dt;

        // Sleep check to reach 0 FPS when settled
        if displacement.abs() < 0.001 && self.velocity.abs() < 0.001 {
            self.position = self.target;
            self.velocity = 0.0;
        }
    }

    pub fn is_at_rest(&self) -> bool {
        (self.position - self.target).abs() < 0.001 && self.velocity.abs() < 0.001
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_spring_converges_to_target() {
        let mut spring = SpringState::new(0.0);
        spring.target = 100.0;
        let config = SpringConfig::default();

        for _ in 0..120 {
            spring.step(0.016, config);
        }

        assert!((spring.position - 100.0).abs() < 0.5);
    }
}
