// Theme management
const themeToggle = document.createElement('button');
themeToggle.className = 'theme-toggle';
themeToggle.innerHTML = '<i class="fas fa-moon"></i>';

// Function to set theme
function setTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
    
    // Update icon
    const icon = themeToggle.querySelector('i');
    if (theme === 'dark') {
        icon.className = 'fas fa-sun';
    } else {
        icon.className = 'fas fa-moon';
    }
}

// Function to toggle theme
function toggleTheme() {
    const currentTheme = localStorage.getItem('theme') || 'light';
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
}

// Initialize theme
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    setTheme(savedTheme);
    
    // Place theme toggle in the appropriate container
    const container = document.querySelector('.container');
    if (container) {
        container.style.position = 'relative'; // Ensure container can position absolute elements
        container.appendChild(themeToggle);
    }
}

// Add event listener
themeToggle.addEventListener('click', toggleTheme);

// Initialize theme when DOM is loaded
document.addEventListener('DOMContentLoaded', initTheme); 