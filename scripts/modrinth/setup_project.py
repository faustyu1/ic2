#!/usr/bin/env python3
"""Creates or updates the Modrinth project page for IC2 Community Edition.

The page (title, summary, description, links, categories, license, icon) is described here and in
description.md next to this script; running it again pushes any change to Modrinth. A new project
is created as a draft: submitting it for review stays a manual step on modrinth.com.

The token is read from MODRINTH_TOKEN or ~/.config/modrinth/token and needs the scopes
Create projects, Read projects and Write projects. Never commit it.

Usage: python3 scripts/modrinth/setup_project.py [--dry-run]
"""
import json
import os
import sys
import urllib.error
import urllib.request
import uuid

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
API = 'https://api.modrinth.com/v2'
USER_AGENT = 'faustyu1/ic2 (modrinth project setup)'
ICON = os.path.join(ROOT, 'src/main/resources/assets/ic2/icon.png')

SLUG = 'ic2-community-edition'
PAGE = {
    'title': 'IndustrialCraft 2 Community Edition',
    'description': 'Unofficial community port of IndustrialCraft 2 to Fabric 1.21.1: EU energy, '
                   'machines, generators, electric tools, crops and UU-matter.',
    'categories': ['technology', 'equipment'],
    'additional_categories': ['food', 'worldgen'],
    'client_side': 'required',
    'server_side': 'required',
    'license_id': 'LGPL-3.0-only',
    'issues_url': 'https://github.com/faustyu1/ic2/issues',
    'source_url': 'https://github.com/faustyu1/ic2',
}


def token():
    value = os.environ.get('MODRINTH_TOKEN')
    if not value:
        path = os.path.expanduser('~/.config/modrinth/token')
        if os.path.isfile(path):
            value = open(path).read().strip()
    if not value:
        sys.exit('setup_project: set MODRINTH_TOKEN or put the token in ~/.config/modrinth/token')
    return value


def request(method, path, auth, body=None, content_type='application/json'):
    req = urllib.request.Request(API + path, data=body, method=method)
    req.add_header('User-Agent', USER_AGENT)
    req.add_header('Authorization', auth)
    if body is not None:
        req.add_header('Content-Type', content_type)
    try:
        with urllib.request.urlopen(req) as response:
            data = response.read()
            return response.status, json.loads(data) if data else None
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', 'replace')


def multipart(data, icon):
    boundary = uuid.uuid4().hex
    parts = [
        ('--%s\r\nContent-Disposition: form-data; name="data"\r\n'
         'Content-Type: application/json\r\n\r\n' % boundary).encode() + json.dumps(data).encode(),
        ('--%s\r\nContent-Disposition: form-data; name="icon"; filename="icon.png"\r\n'
         'Content-Type: image/png\r\n\r\n' % boundary).encode() + icon,
    ]
    body = b'\r\n'.join(parts) + ('\r\n--%s--\r\n' % boundary).encode()
    return body, 'multipart/form-data; boundary=' + boundary


def check(status, result, what):
    if status >= 300:
        sys.exit('setup_project: %s failed (HTTP %d): %s' % (what, status, result))


def main():
    dry_run = '--dry-run' in sys.argv[1:]
    page = dict(PAGE, body=open(os.path.join(HERE, 'description.md'), encoding='utf-8').read())
    icon = open(ICON, 'rb').read()
    if dry_run:
        print(json.dumps(dict(page, slug=SLUG, body='<%d chars>' % len(page['body'])), indent=2))
        print('icon: %s (%d bytes)' % (ICON, len(icon)))
        return

    auth = token()
    status, project = request('GET', '/project/' + SLUG, auth)
    if status == 404:
        data = dict(page, slug=SLUG, project_type='mod', initial_versions=[], is_draft=True)
        body, content_type = multipart(data, icon)
        status, project = request('POST', '/project', auth, body, content_type)
        check(status, project, 'creating the project')
        print('created draft project %s (id %s)' % (SLUG, project['id']))
        return
    check(status, project, 'looking up the project')

    status, result = request('PATCH', '/project/' + project['id'], auth, json.dumps(page).encode())
    check(status, result, 'updating the page')
    status, result = request('PATCH', '/project/%s/icon?ext=png' % project['id'], auth, icon, 'image/png')
    check(status, result, 'updating the icon')
    print('updated project %s (id %s, status %s)' % (SLUG, project['id'], project['status']))


if __name__ == '__main__':
    main()
