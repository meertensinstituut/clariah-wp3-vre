import React from "react";

/**
 * Expects response body with msg field containing error
 */
export default class ErrorMsg extends React.Component {
    constructor(props, context) {
        super(props, context);
        this.state = {
            show: true,
            response: this.props.response
        };
    }

    /**
     * If props change, show error msg again
     */
    static getDerivedStateFromProps(nextProps, prevState) {
        if(nextProps.response === prevState.response) {
            return {};
        }
        return {response: nextProps.response, show: true};
    }

    handleDismiss = () => {
        this.setState({show: false});
    };

    render() {
        if (!this.state.show) {
            return null;
        }
        if (!this.props.response) {
            return null;
        }
        return (
            <div role="alert" className="alert alert-danger alert-dismissable">
                <button type="button" className="pull-right" onClick={() => this.handleDismiss()}>
                    <span aria-hidden="true">×</span>
                </button>
                <h4>Error</h4>
                <p>{this.state.response.msg}</p>
            </div>
        );
    }
}

ErrorMsg.defaultTypes = {
    response: null
};
