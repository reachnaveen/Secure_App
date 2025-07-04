import React, { useState, useEffect } from 'react';
import './App.css';

function App() {
  const [message, setMessage] = useState('');
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch('/api/hello')
      .then(response => {
        if (response.ok) {
          return response.text();
        }
        throw new Error('Not authenticated');
      })
      .then(data => setMessage(data))
      .catch(err => setError(err.message));
  }, []);

  return (
    <div className="App">
      <header className="App-header">
        {error ? (
          <div>
            <p>Please log in to see the message.</p>
            <a className="App-link" href="/oauth2/authorization/google">
              Log in with Google
            </a>
          </div>
        ) : (
          <p>{message}</p>
        )}
      </header>
    </div>
  );
}

export default App;
