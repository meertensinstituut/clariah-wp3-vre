import React from 'react';
import ReactDOM from 'react-dom';
import Page from './app/page';

import './index.css';
import 'font-awesome/css/font-awesome.min.css';
import {BrowserRouter} from "react-router-dom";

ReactDOM.render(
    <BrowserRouter>
        <Page/>
    </BrowserRouter>,
    document.getElementById('root')
);
