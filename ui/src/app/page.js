import React from 'react';
import {Col, Grid, Row} from 'react-bootstrap';
import Main from './main';
import Navigation from './navigation';
import {VreLayout} from "../react-gui/clariah-vre";

export default class Page extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            title: 'VRE',
            subtitle: 'A CLARIAH WP3 Virtual Research Environment',
        };
    }

    render() {
        return (
            <VreLayout>
                <Grid>
                    <Row>
                        <Col xs={12} md={12} className='header'>
                            <h1>{this.state.title}</h1>
                            <p>{this.state.subtitle}</p>
                        </Col>
                    </Row>
                    <Row>
                        <Col xs={12} md={12} className='content'>
                            <Navigation />
                        </Col>
                    </Row>
                    <Row>
                        <Col xs={12} md={12} className='content'>
                            <Main/>
                        </Col>
                    </Row>
                </Grid>
            </VreLayout>
        );
    }
}
