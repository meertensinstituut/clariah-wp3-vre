import React from "react";
import {Redirect, withRouter} from 'react-router-dom';
import queryString from 'query-string';
import {Pagination} from 'react-bootstrap';
import ServiceSelector from "./service-selector";
import Configurator from "./configurator";

class Deploy extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            params: queryString.parse(this.props.location.search),
            redirect: null,
            serviceParams: null,
        };

        this.handleSelect = this.handleSelect.bind(this);
    }

    handleBackClick() {
        this.setState({redirect: "/files"});
    }

    handleBackToServiceClick() {
        const params = this.state.params;
        delete params.service;
        this.props.history.push('/deploy?' + queryString.stringify(params));
        this.setState({params});
    }

    handleSelect(service) {
        let params = this.state.params;
        params.service = service.id;
        this.props.history.push('/deploy?' + queryString.stringify(params));
        this.setState({params});
    }

    isCurrentStep(page, nextPage) {
        return this.state.params[page] !== undefined && this.state.params[nextPage] === undefined;
    }

    isDisabledStep(previousPage) {
        return this.state.params[previousPage] === undefined;
    }

    handleValidConfig = (config) => {
        console.log("handleValidConfig", config);
    };

    render() {
        if (this.state.redirect !== null) return <Redirect to='/files'/>;

        if (this.state.params.file === undefined) return <Redirect to="/files"/>;

        const selectService = this.isCurrentStep('file', 'service')
            ?
            <div>
                <h2>1. Select service</h2>
                <ServiceSelector
                    file={this.state.params.file}
                    selected={this.state.params.service}
                    onSelect={this.handleSelect}
                />
            </div>
            :
            null;

        const configureService = this.isCurrentStep('service', 'params')
            ?
            <div>
                <h2>2. Configure service</h2>
                <Configurator
                    service={this.state.params.service}
                    onValidConfig={this.handleValidConfig}
                />
            </div>
            :
            null;

        return (
            <div>
                <div>
                    <Pagination>
                        <Pagination.Item
                            onClick={() => this.handleBackClick()}
                        >
                            &lt; Select file
                        </Pagination.Item>
                        <Pagination.Item
                            active={this.isCurrentStep('file', 'service')}
                            disabled={this.isDisabledStep('file')}
                            onClick={() => this.handleBackToServiceClick()}
                        >
                            Select service
                        </Pagination.Item>
                        <Pagination.Item
                            active={this.isCurrentStep('service', 'params')}
                            disabled={this.isDisabledStep('service')}
                        >
                            Configure service
                        </Pagination.Item>
                        <Pagination.Item
                            active={this.isCurrentStep('params', true)}
                            disabled={this.isDisabledStep('params')}
                        >
                            Deploy service &gt;
                        </Pagination.Item>
                    </Pagination>
                </div>
                {selectService}
                <div className="clearfix"/>
                {configureService}

            </div>
        );
    }
}

export default withRouter(Deploy);