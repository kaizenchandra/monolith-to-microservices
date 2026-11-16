import http from 'k6/http';
import {check} from 'k6';

export const options = {
    summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)'],
    vus: 20,
    duration: '30s',
    thresholds: {http_req_failed: ['rate<0.01']}
};
export default function () {
    const r = http.get(`${__ENV.BASE_URL || 'http://localhost:28082'}/api/products/1`, {headers: {Authorization: 'Basic ' + encoding()}});
    check(r, {'200': r => r.status === 200});
}

function encoding() {
    return 'cmVhZGVyOmxvY2FsLW9ubHk=';
}
