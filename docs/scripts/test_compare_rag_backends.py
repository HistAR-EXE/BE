"""Offline contract tests. No Docker, database, or model calls are made."""
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch, Mock

SCRIPT = Path(__file__).with_name('compare-rag-backends.py')
spec = importlib.util.spec_from_file_location('compare_rag_backends', SCRIPT)
h = importlib.util.module_from_spec(spec) if SCRIPT.exists() else None
if h is not None:
    spec.loader.exec_module(h)

SOURCE = {'title': 'Cedar Falls', 'excerpt': 'Cedar Falls năm 1967 tại Củ Chi.', 'url': None}
GOOD = {'reply': 'Chiến dịch Cedar Falls năm 1967 tại Củ Chi.', 'sources': [SOURCE], 'errors': []}


class HarnessTests(unittest.TestCase):
    def setUp(self):
        self.assertIsNotNone(h, 'The acceptance harness has not been implemented yet')

    def test_loopback_guard(self):
        for url in ('http://localhost:18081', 'http://127.0.0.1:18082', 'http://[::1]:18083'):
            self.assertTrue(h.local_url(url, backend=True).startswith('http://'))
        for url in ('http://example.com:18081', 'http://127.0.0.1:8080',
                    'http://localhost:8080', 'http://localhost.evil:99',
                    'http://user:pw@localhost:99', 'http://localhost:99/x',
                    'http://localhost:99?x=1', 'file:///tmp/test', 'http://0.0.0.0:99'):
            with self.subTest(url=url), self.assertRaises(ValueError):
                h.local_url(url, backend=True)

    def test_disposable_database_guard(self):
        self.assertEqual(h.database_name('timelens_rag_test_unit'), 'timelens_rag_test_unit')
        for db in ('timelens', 'timelens_rag_test_', 'timelens_rag_test_x;DROP DATABASE x',
                   'timelens_rag_test_' + 'x' * 64):
            with self.assertRaises(ValueError):
                h.database_name(db)
        with self.assertRaises(ValueError):
            h.Database('timelens_rag_test_unit', 'wrong-container', 'timelens')
        with self.assertRaises(ValueError):
            h.Database('timelens_rag_test_unit', 'histar-postgres', 'postgres')

    def test_psql_uses_utf8_stdin_and_hides_error_secrets(self):
        db = h.Database('timelens_rag_test_unit', 'histar-postgres', 'timelens')
        with patch.object(h.subprocess, 'run', return_value=Mock(returncode=0, stdout=b'ok\n')) as run:
            self.assertEqual(db.sql("SELECT 'Củ Chi';"), 'ok')
            args, kwargs = run.call_args
            self.assertEqual(args[0][:4], ['docker', 'exec', '-i', 'histar-postgres'])
            self.assertIn('ON_ERROR_STOP=1', args[0])
            self.assertEqual(kwargs['input'], "SET client_encoding TO 'UTF8';\nSELECT 'Củ Chi';".encode())
            self.assertNotIn('shell', kwargs)
        with patch.object(h.subprocess, 'run', return_value=Mock(returncode=1, stderr=b'password-secret')):
            with self.assertRaisesRegex(RuntimeError, 'psql failed') as raised:
                db.sql('password-secret')
            self.assertNotIn('password-secret', str(raised.exception))

    def test_fixtures_random_and_followup_history_identical(self):
        history_text = ' '.join(content for _, content in h.HISTORY).casefold()
        self.assertIn('cedar falls', history_text)
        self.assertIn('1967', history_text)
        db = h.Database('timelens_rag_test_unit', 'histar-postgres', 'timelens')
        with patch.object(db, 'sql') as sql:
            user, email, password = db.profile()
            h.uuid.UUID(user)
            self.assertIn('@example.invalid', email)
            self.assertGreaterEqual(len(password), 32)
            statement = sql.call_args.args[0]
            self.assertIn("gen_salt('bf')", statement)
            self.assertIn("'ADMIN', 'FREE', TRUE", statement)
            first = db.conversation(user, user, True)
            first_sql = sql.call_args.args[0]
            second = db.conversation(user, user, True)
            second_sql = sql.call_args.args[0]
            self.assertNotEqual(first, second)
            for _, content in h.HISTORY:
                self.assertIn(content, first_sql)
                self.assertIn(content, second_sql)
            self.assertIn('created_at', first_sql)
            self.assertIn("'2000-01-01T00:00:01Z'", first_sql)

    def test_sync_and_login_envelopes(self):
        self.assertEqual(h.login_token({'data': {'accessToken': 'a'}}), 'a')
        self.assertEqual(h.login_token({'data': {'token': 'b'}}), 'b')
        with self.assertRaises(ValueError):
            h.login_token({'data': {}})
        result = h.parse_sync(json.dumps({'data': GOOD}))
        self.assertEqual(result['reply'], GOOD['reply'])
        self.assertEqual(result['sources'], [SOURCE])
        self.assertTrue(h.parse_sync('not JSON')['errors'])
        self.assertTrue(h.parse_sync('{"data": null}')['errors'])

    def test_http_request_uses_event_stream_accept_for_sse(self):
        response = Mock(status=200)
        response.read.return_value = b'event: complete\ndata: {}\n\n'
        response.__enter__ = Mock(return_value=response)
        response.__exit__ = Mock(return_value=False)
        with patch.object(h.urllib.request, 'urlopen', return_value=response) as urlopen:
            status, _ = h._request(
                'http://127.0.0.1:18081', '/api/chat/messages/stream', {},
                accept='text/event-stream')
        self.assertEqual(status, 200)
        request = urlopen.call_args.args[0]
        self.assertEqual(request.get_header('Accept'), 'text/event-stream')

    def test_sse_multiline_unicode_dedupe_and_terminal(self):
        raw = ': ping\r\nevent: meta\r\ndata: {"requestId":"r"}\r\n\r\n'
        raw += 'event: delta\ndata: {"content":"Củ Chi",\ndata: "sources":[' + json.dumps(SOURCE) + ']}\n\n'
        raw += 'event: sources\ndata: ' + json.dumps([SOURCE]) + '\n\n'
        raw += 'event: complete\ndata: {"conversationId":"c"}'
        result = h.parse_sse(raw)
        self.assertEqual(result['reply'], 'Củ Chi')
        self.assertEqual(result['sources'], [SOURCE])
        self.assertEqual(result['requestId'], 'r')
        self.assertTrue(result['complete'])
        self.assertEqual(result['errors'], [])
        self.assertTrue(h.parse_sse('event: delta\ndata: {"content":"partial"}\n\n')['errors'])
        self.assertTrue(h.parse_sse('event: delta\ndata: broken\n\n')['errors'])

    def test_expected_error_requires_structured_server_failure(self):
        error = {'code': 'RAG_UNAVAILABLE', 'retryable': True, 'requestId': 'r'}
        parsed = h.parse_sse('event: error\ndata: ' + json.dumps(error) + '\n\n')
        self.assertEqual(h.validate_error(parsed, 200, 'RAG_UNAVAILABLE', 'r'), [])
        self.assertTrue(h.validate_error(parsed, 200, 'RAG_TIMEOUT', 'r'))
        self.assertTrue(h.validate_error(parsed, None, 'RAG_UNAVAILABLE', 'r'))
        self.assertTrue(h.validate_error(GOOD, 200, 'RAG_UNAVAILABLE', 'r'))
        parsed['reply'] = 'pretend success'
        self.assertTrue(h.validate_error(parsed, 200, 'RAG_UNAVAILABLE', 'r'))

    def test_golden_validation_semantics(self):
        self.assertEqual(h.validate('cedar_short', GOOD), [])
        self.assertEqual(h.validate('cedar_full', {
            **GOOD, 'reply': 'Chiến dịch Cedar Falls là một cuộc càn quét quy mô lớn tại Củ Chi.'
        }), [])
        self.assertTrue(h.validate('cedar_full', {**GOOD, 'reply': 'Xin chào! Tôi có thể giúp gì?'}))
        self.assertTrue(h.validate('cedar_full', {**GOOD, 'sources': []}))
        self.assertTrue(h.validate('cedar_full', {**GOOD, 'reply': 'Cedar Falls năm 1968.'}))
        self.assertTrue(h.validate('cedar_full', {**GOOD, 'sources': [{'title': 'Cooking', 'excerpt': 'Rice'}]}))
        unknown = {'reply': 'Tư liệu hiện có chưa đủ thông tin để xác minh mã hồ sơ này.', 'sources': [], 'errors': []}
        self.assertEqual(h.validate('no_source', unknown), [])
        self.assertTrue(h.validate('no_source', {**unknown, 'sources': [SOURCE]}))
        self.assertTrue(h.validate('no_source', {**unknown, 'reply': 'Hồ sơ này có thật, lập năm 1967 [1].'}))

    def test_comparison_uses_sources_and_facts_not_wording(self):
        other = {**GOOD, 'reply': 'Tại Củ Chi, Cedar Falls diễn ra vào năm 1967.'}
        result = h.compare(GOOD, other)
        self.assertEqual(result['sourceOverlap'], 1.0)
        self.assertTrue(result['passed'])
        self.assertIn('year_1967', result['sharedSupportedFacts'])
        self.assertFalse(h.compare(GOOD, {**other, 'sources': []})['passed'])
        no_source = {'reply': 'Tư liệu hiện có chưa đủ thông tin để xác minh.',
                     'sources': [], 'errors': []}
        self.assertTrue(h.compare(no_source, no_source)['passed'])

    def test_diagnostics_join_and_missing_evidence(self):
        raw = 'INFO chat_complete request_id=r answer_source=llm diagnostics={"model":"m","sourceIds":["s"]}\n'
        raw += 'INFO request_id=other diagnostics={"model":"wrong"}\n'
        joined = h.diagnostics(raw, 'r')
        self.assertEqual(joined['model'], 'm')
        self.assertEqual(joined['sourceIds'], ['s'])
        self.assertEqual(joined['missing'], [])
        self.assertIn('diagnostics', h.diagnostics(raw, 'missing')['missing'])
        self.assertIn('model', h.diagnostics('request_id=r diagnostics={"sourceIds":[]}', 'r')['missing'])

    def test_cli_two_distinct_backends_or_explicit_error_target(self):
        common = ['--main', 'http://localhost:18081', '--ai', 'http://localhost:8000',
                  '--db', 'timelens_rag_test_unit', '--docker-container', 'histar-postgres',
                  '--db-user', 'timelens', '--output', 'report.json']
        args = h.arguments(common + ['--snapshot', 'http://localhost:18082'])
        self.assertEqual(args.warm_passes, 1)
        with self.assertRaises(ValueError):
            h.arguments(common + ['--snapshot', 'http://127.0.0.1:18081'])
        with self.assertRaises(ValueError):
            h.arguments(common)
        self.assertEqual(h.arguments(common + ['--expected-error', 'RAG_UNAVAILABLE']).expected_error, 'RAG_UNAVAILABLE')


if __name__ == '__main__':
    unittest.main()
