import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';
import { SUPABASE_URL, SUPABASE_ANON_KEY } from './config.js';

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
const EMAIL_REDIRECT_TO = `${window.location.origin}/auth.html`;

const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');
const passwordInput = document.getElementById('rPassword');
const confirmInput = document.getElementById('rConfirmPassword');
const passwordErrorEl = document.getElementById('passwordError');
const confirmErrorEl = document.getElementById('confirmError');
const toggleBtn = document.getElementById('toggleBtn');
const toggleText = document.getElementById('toggleText');
const toast = document.getElementById('toast');
const toastMessage = document.getElementById('toastMessage');

function showToast(message) {
  if (!toast || !toastMessage) return;
  toastMessage.textContent = message;
  // Ensure visible and animate in
  toast.classList.remove('hidden');
  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 3000);
}

function toggleForms() {
  const isLoginVisible = !loginForm.classList.contains('hidden');
  if (isLoginVisible) {
    loginForm.classList.add('hidden');
    registerForm.classList.remove('hidden');
    toggleText.textContent = 'Already have an account?';
    toggleBtn.textContent = 'Sign in';
  } else {
    registerForm.classList.add('hidden');
    loginForm.classList.remove('hidden');
    toggleText.textContent = "Don't have an account?";
    toggleBtn.textContent = 'Sign up';
  }
}

toggleBtn.addEventListener('click', (e) => {
  e.preventDefault();
  toggleForms();
});

loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;
  if (!email || !password) {
    showToast('Please enter email and password');
    return;
  }
  try {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password });
    if (error) {
      console.error('Sign in error:', error);
      throw error;
    }
    showToast('Signed in successfully');
    // Redirect to app
    window.location.href = 'index.html';
  } catch (err) {
    const msg = err?.message || 'Sign in failed';
    showToast(msg);
  }
});

registerForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const email = document.getElementById('rEmail').value.trim();
  const password = passwordInput.value;
  const confirmPassword = confirmInput.value;
  if (!email || !password) {
    showToast('Please enter email and password');
    return;
  }
  const pwdMsg = validatePassword(password);
  const matchMsg = validateConfirm(password, confirmPassword);
  const hasPwdError = !!pwdMsg;
  const hasMatchError = !!matchMsg;
  setFieldError(passwordErrorEl, pwdMsg);
  setFieldError(confirmErrorEl, matchMsg);
  if (hasPwdError || hasMatchError) return;
  try {
    // Standard Supabase sign-up (configure confirmation behavior in Dashboard)
    const { data, error } = await supabase.auth.signUp({
      email,
      password,
      options: { emailRedirectTo: EMAIL_REDIRECT_TO }
    });
    if (error) {
      console.error('Sign up error:', error);
      const rawMsg = error?.message || '';
      const normalized = rawMsg.toLowerCase();
      const friendly = normalized.includes('already') || normalized.includes('registered') || normalized.includes('exists')
        ? 'Email already exists. Try signing in or use a different email.'
        : rawMsg || 'Sign up failed';
      throw new Error(friendly);
    }
    showToast('Account created successfully.');
    toggleForms();
  } catch (err) {
    const msg = err?.message || 'Sign up failed';
    showToast(msg);
  }
});

// If already logged in, go to index
(async () => {
  const { data: { session } } = await supabase.auth.getSession();
  if (session) {
    window.location.href = 'index.html';
  }
})();
function validatePassword(pwd) {
  const rules = [];
  if (!pwd || pwd.length < 6) rules.push('at least 6 characters');
  if (!/[A-Za-z]/.test(pwd)) rules.push('a letter');
  if (!/[0-9]/.test(pwd)) rules.push('a number');
  return rules.length ? `Password must include ${rules.join(', ')}` : '';
}

function validateConfirm(pwd, confirmPwd) {
  if (!confirmPwd) return 'Please confirm your password';
  if (pwd !== confirmPwd) return 'Passwords do not match';
  return '';
}

function setFieldError(el, message) {
  if (!el) return;
  if (message) {
    el.textContent = message;
    el.classList.remove('hidden');
  } else {
    el.textContent = '';
    el.classList.add('hidden');
  }
}

// Live validation
if (passwordInput) {
  passwordInput.addEventListener('input', () => {
    setFieldError(passwordErrorEl, validatePassword(passwordInput.value));
  });
}
if (confirmInput) {
  confirmInput.addEventListener('input', () => {
    setFieldError(confirmErrorEl, validateConfirm(passwordInput.value, confirmInput.value));
  });
}

// Resend confirmation email for the provided email (from either form)
document.getElementById('resendBtn').addEventListener('click', async (e) => {
  e.preventDefault();
  const email = (document.getElementById('rEmail').value.trim() || document.getElementById('email').value.trim());
  if (!email) {
    showToast('Enter your email first');
    return;
  }
  try {
    const { data, error } = await supabase.auth.resend({ type: 'signup', email });
    if (error) {
      console.error('Resend error:', error);
      throw error;
    }
    showToast('Confirmation email sent');
  } catch (err) {
    const msg = err?.message || 'Resend failed';
    showToast(msg);
  }
});
