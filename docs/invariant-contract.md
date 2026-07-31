# Clockwork Invariant Contract

Every vertical slice must preserve these guarantees:

- **Durable**: Any job acknowledged by `POST /jobs` is persisted before `202 Accepted` is returned, so the job survives process restart.
- **Bounded lag**: Jobs scheduled for `T` should execute within the lag budget (`p99 <= 1 minute` initial target).
- **No double-fire within slice scope**: In the current single-node skeleton, each due job is claimed once and marked `DONE` only once.
- **Backpressure-safe**: A slow callback consumer must not crash the scheduler. When the in-memory queue is full, claimed jobs are requeued as `PENDING`.
- **HTTP-callback only**: Clockwork posts payloads to client callback URLs and never executes client code in-process.

These invariants are test targets for the walking-skeleton slices.
