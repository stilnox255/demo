import { test as setup } from '@playwright/test';
import { authenticate } from '../auth-helper.js';

setup('authenticate as admin', async ({ page }) => {
    await authenticate(page, 'admin');
});
