import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';
const SHOW_ID = __ENV.SHOW_ID || '1';
const SEAT_ID = __ENV.SEAT_ID || '1';

const successfulBookings = new Counter('successful_bookings');
const conflicts = new Counter('booking_conflicts');
const failures = new Counter('booking_failures');
const bookingTime = new Trend('booking_response_time');

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<5000'],
  },
};

export default function () {
  const response = http.post(
    `${BASE_URL}/api/shows/${SHOW_ID}/reservations`,
    JSON.stringify({
      seatIds: [Number(SEAT_ID)]
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${TOKEN}`,
        'Idempotency-Key': `k6-${__VU}-${__ITER}-${Date.now()}`
      },
    }
  );

  bookingTime.add(response.timings.duration);

  if (response.status === 201) {
    successfulBookings.add(1);
  } else if (response.status === 409) {
    conflicts.add(1);
  } else {
    failures.add(1);
  }

  check(response, {
    'status is 201 or 409': r =>
      r.status === 201 || r.status === 409,
  });

  sleep(0.2);
}