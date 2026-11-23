import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { loadConfigFromFile } from 'vite'

const root = path.dirname(fileURLToPath(import.meta.url))
const workspace = path.resolve(root, '../../..')
const matches = (key, url) => key.startsWith('^') ? new RegExp(key).test(url) : url.startsWith(key)

for (const local of [false, true]) {
    test(`actual Vite config preserves routing and aliases (LOCAL=${local})`, async () => {
        const previous = process.env.LOCAL_SIBLINGS
        let config
        try {
            process.env.LOCAL_SIBLINGS = local ? '1' : '0'
            config = (await loadConfigFromFile({ command: 'serve', mode: 'test' }, path.join(root, 'vite.config.js'), root)).config
        } finally {
            if (previous === undefined) delete process.env.LOCAL_SIBLINGS
            else process.env.LOCAL_SIBLINGS = previous
        }
        assert.equal(config.base, '/script/')
        const proxy = config.server.proxy
        assert.deepEqual(Object.keys(proxy), ['^/script/api(?:/|$)'])
        const matching = url => Object.keys(proxy).filter(key => matches(key, url))
        for (const url of ['/script/api', '/script/api/script/list', '/script/api/mock-platform/endpoints/list', '/script/api/script/app/list?apiKey=fixture']) {
            assert.equal(matching(url).length, 1, url)
        }
        for (const url of ['/script/', '/script/z-script/home', '/script/z-script/console', '/script/z-script/docs', '/script/z-script/login', '/script/src/main.jsx', '/script/assets/app.js', '/script/icon.png', '/script/@vite/client', '/script/apix/list', '/src/main.jsx']) {
            assert.equal(matching(url).length, 0, url)
        }
        assert.equal(proxy['^/script/api(?:/|$)'].target, 'http://localhost:8086')
        assert.equal(proxy['^/script/api(?:/|$)'].changeOrigin, true)
        assert.equal(proxy['^/script/api(?:/|$)'].rewrite, undefined)
        assert.deepEqual(config.server.fs.allow, [local ? workspace : path.resolve(root, '..')])
        const aliases = config.resolve.alias
        assert.equal(aliases.find(a => a.find === '@').replacement, path.join(root, 'src'))
        const siblings = aliases.filter(a => a.find instanceof RegExp)
        assert.equal(siblings.length, local ? 4 : 0)
        if (local) {
            const expected = {
                '@yuku123/z-script-component/pages': path.resolve(root, '../z-script-component/src/pages.js'),
                '@yuku123/z-script-component': path.resolve(root, '../z-script-component/src/index.jsx'),
                '@yuku123/z-frontend-common/ui': path.join(workspace, 'z-frontend-common/src/components/ui/index.js'),
                '@yuku123/z-frontend-common': path.join(workspace, 'z-frontend-common/src/index.js'),
            }
            for (const [specifier, file] of Object.entries(expected)) {
                const hits = siblings.filter(a => a.find.test(specifier))
                assert.equal(hits.length, 1, specifier)
                assert.equal(hits[0].replacement, file)
                assert.ok(path.isAbsolute(file) && existsSync(file), file)
                assert.equal(hits[0].find.test(`${specifier}/unrelated`), false)
            }
        }
    })
}
